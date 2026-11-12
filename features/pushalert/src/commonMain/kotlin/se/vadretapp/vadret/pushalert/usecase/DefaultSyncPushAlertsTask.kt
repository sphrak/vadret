package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.encryption.SealedMessageKey
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences
import se.vadretapp.vadret.pushalert.DeviceTokenSource
import se.vadretapp.vadret.pushalert.NotificationPermissionSource
import se.vadretapp.vadret.pushalert.PushAlertRepository
import se.vadretapp.vadret.pushalert.extension.notifiedAllergenIds
import se.vadretapp.vadret.pushalert.model.DeviceRegistration
import se.vadretapp.vadret.pushalert.model.DeviceToken
import se.vadretapp.vadret.pushalert.model.InstallationToken
import se.vadretapp.vadret.pushalert.model.PlaceName
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PollenChoice
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertSubscription
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.Language
import se.vadretapp.vadret.setting.usecase.GetHourFormatTask
import se.vadretapp.vadret.setting.usecase.GetShownLanguageTask
import kotlin.io.encoding.Base64
import kotlin.time.Clock

internal class DefaultSyncPushAlertsTask(
    private val pushAlertRepository: PushAlertRepository,
    private val pollenPreferenceRepository: PollenPreferenceRepository,
    private val forecastPreferenceRepository: ForecastPreferenceRepository,
    private val getHourFormatTask: GetHourFormatTask,
    private val getShownLanguageTask: GetShownLanguageTask,
    private val deviceTokenSource: DeviceTokenSource,
    private val notificationPermissionSource: NotificationPermissionSource,
    private val sealedMessageKey: SealedMessageKey,
    private val getCurrentGeohashTask: GetCurrentGeohashTask,
    private val getPushAlertPlaceNamesTask: GetPushAlertPlaceNamesTask,
    private val clock: Clock
) : SyncPushAlertsTask {
    private val syncMutex = Mutex()

    override suspend fun invoke(): Either<Failure, Unit> = syncMutex.withLock {
        when (val synced = syncAndInstallAgainIfForgotten()) {
            is Either.Left -> {
                keepFailure(failure = synced.a)
                synced
            }
            is Either.Right -> pushAlertRepository.saveIsSyncPending(isSyncPending = false)
        }
    }

    private suspend fun syncAndInstallAgainIfForgotten(): Either<Failure, Unit> {
        val synced = sync()
        if (synced is Either.Left && synced.a.errorCode == ErrorCode.HTTP_UNAUTHORIZED) {
            logger.warn { "The server no longer knows this installation, so it installs again" }
            return pushAlertRepository.clearInstallation().flatMap { sync() }
        }
        return synced
    }

    private suspend fun keepFailure(failure: Failure) {
        if (failure.errorCode == ErrorCode.ATTESTATION_REFUSED) {
            logger.warn { "${ErrorCode.ATTESTATION_REFUSED}: the server could not confirm this phone, so every push alert is turned off" }
            pushAlertRepository.saveChoices(pushAlertChoices = PushAlertChoices.NOTHING_CHOSEN)
            pushAlertRepository.saveIsSyncPending(isSyncPending = false)
        } else {
            pushAlertRepository.saveIsSyncPending(isSyncPending = true)
        }
    }

    private suspend fun sync(): Either<Failure, Unit> =
        followedChoices().flatMap { pushAlertChoices: PushAlertChoices ->
            if (pushAlertChoices.isAnythingChosen() && isNotificationAllowed()) {
                follow(pushAlertChoices = pushAlertChoices)
            } else {
                forget()
            }
        }

    // Aurora is a lab, so the server only sends aurora while the lab is on, and the stored choice comes back with it.
    private suspend fun followedChoices(): Either<Failure, PushAlertChoices> =
        pushAlertRepository.choices().flatMap { pushAlertChoices: PushAlertChoices ->
            forecastPreferenceRepository.getForecastPreferences().map { forecastPreferences: ForecastPreferences ->
                pushAlertChoices.copy(aurora = pushAlertChoices.aurora && forecastPreferences.isAuroraEnabled)
            }
        }

    private suspend fun isNotificationAllowed(): Boolean {
        val isNotificationAllowed = notificationPermissionSource.isNotificationAllowed()
        if (!isNotificationAllowed) {
            logger.info { "Notifications are not allowed on this phone, so it stays unknown to the server" }
        }
        return isNotificationAllowed
    }

    private suspend fun forget(): Either<Failure, Unit> =
        pushAlertRepository.installationToken().flatMap { installationToken: InstallationToken? ->
            if (installationToken == null) {
                Unit.asRight()
            } else {
                pushAlertRepository.forgetInstallation(installationToken = installationToken)
            }
        }

    private suspend fun follow(pushAlertChoices: PushAlertChoices): Either<Failure, Unit> =
        installation().flatMap { installationToken: InstallationToken ->
            registerDevice(installationToken = installationToken).flatMap {
                getCurrentGeohashTask().flatMap { currentGeohash: Geohash ->
                    subscribe(
                        installationToken = installationToken,
                        currentGeohash = currentGeohash,
                        pushAlertChoices = pushAlertChoices
                    )
                }
            }
        }

    private suspend fun subscribe(
        installationToken: InstallationToken,
        currentGeohash: Geohash,
        pushAlertChoices: PushAlertChoices
    ): Either<Failure, Unit> =
        getPushAlertPlaceNamesTask(
            currentGeohash = currentGeohash,
            pushAlertChoices = pushAlertChoices
        ).flatMap { placeNames: Map<Geohash, PlaceName?> ->
            if (placeNames.isEmpty()) {
                logger.info { "No place is chosen for push alerts, so the server forgets this phone" }
                forget()
            } else {
                pollenChoices(pollenLowestLevel = pushAlertChoices.pollenLowestLevel).flatMap { pollenChoices: List<PollenChoice> ->
                    getHourFormatTask().flatMap { hourFormat: HourFormat ->
                        getShownLanguageTask().flatMap { language: Language ->
                            pushAlertRepository.replaceSubscription(
                                installationToken = installationToken,
                                pushAlertSubscription = PushAlertSubscription(
                                    geohashes = placeNames.keys.toList(),
                                    placeNames = placeNames,
                                    dailyGeohash = if (pushAlertChoices.isDailyForecastChosen() && currentGeohash in placeNames) {
                                        currentGeohash
                                    } else {
                                        null
                                    },
                                    warningLowestLevel = pushAlertChoices.warningLowestLevel,
                                    pollenChoices = pollenChoices,
                                    temperatureTrend = pushAlertChoices.temperatureTrend,
                                    strongWind = pushAlertChoices.strongWind,
                                    slipperiness = pushAlertChoices.slipperiness,
                                    aurora = pushAlertChoices.aurora,
                                    hourFormat = hourFormat,
                                    language = language
                                )
                            )
                        }
                    }
                }.flatMap { pushAlertRepository.saveSyncedCurrentGeohash(geohash = currentGeohash) }
            }
        }

    // The push alert settings only pick how high the pollen must reach; which pollen is the allergens picked for pollen.
    private suspend fun pollenChoices(pollenLowestLevel: PollenAlertLevel?): Either<Failure, List<PollenChoice>> =
        if (pollenLowestLevel == null) {
            emptyList<PollenChoice>().asRight()
        } else {
            pollenPreferenceRepository.getPollenPreferences().map { pollenPreferences: PollenPreferences ->
                pollenPreferences.notifiedAllergenIds().sorted().map { pollenTypeId: String ->
                    PollenChoice(
                        pollenTypeId = pollenTypeId,
                        lowestLevel = pollenLowestLevel
                    )
                }
            }
        }

    private suspend fun installation(): Either<Failure, InstallationToken> =
        pushAlertRepository.installationToken().flatMap { installationToken: InstallationToken? ->
            if (installationToken == null) {
                pushAlertRepository.install()
            } else {
                installationToken.asRight()
            }
        }

    private suspend fun registerDevice(installationToken: InstallationToken): Either<Failure, Unit> =
        pushAlertRepository.registeredDevice().flatMap { deviceRegistration: DeviceRegistration? ->
            devicePublicKey().flatMap { devicePublicKey: ByteArray ->
                deviceTokenSource.deviceToken().flatMap { deviceToken: DeviceToken ->
                    val nowMillis = clock.now().toEpochMilliseconds()
                    val isAlreadyRegistered = deviceRegistration != null &&
                        deviceRegistration.deviceToken == deviceToken &&
                        deviceRegistration.devicePublicKey == Base64.encode(source = devicePublicKey) &&
                        nowMillis - deviceRegistration.registeredAtMillis < REGISTRATION_REFRESH_MILLIS
                    if (isAlreadyRegistered) {
                        Unit.asRight()
                    } else {
                        pushAlertRepository.registerDevice(
                            installationToken = installationToken,
                            deviceToken = deviceToken,
                            replacesDeviceToken = deviceRegistration?.deviceToken,
                            devicePublicKey = devicePublicKey,
                            registeredAtMillis = nowMillis
                        )
                    }
                }
            }
        }

    private fun devicePublicKey(): Either<Failure, ByteArray> =
        when (val publicKey = sealedMessageKey.publicKey()) {
            is Either.Left -> {
                logger.warn { "${ErrorCode.STORAGE_FAILED}: the key that opens push alerts cannot be used: ${publicKey.a}" }
                Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft()
            }
            is Either.Right -> publicKey
        }
}

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pushalert")

private const val REGISTRATION_REFRESH_MILLIS = 2_592_000_000L