package fi.kroon.vadret.pushalert.usecase

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.encryption.SealedMessageKey
import fi.kroon.vadret.geohash.model.Geohash
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import fi.kroon.vadret.pushalert.DeviceTokenSource
import fi.kroon.vadret.pushalert.NotificationPermissionSource
import fi.kroon.vadret.pushalert.PushAlertRepository
import fi.kroon.vadret.pushalert.model.DeviceRegistration
import fi.kroon.vadret.pushalert.model.DeviceToken
import fi.kroon.vadret.pushalert.model.InstallationToken
import fi.kroon.vadret.pushalert.model.PushAlertChoices
import fi.kroon.vadret.pushalert.model.PushAlertSubscription
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.io.encoding.Base64
import kotlin.time.Clock

internal class DefaultSyncPushAlertsTask(
    private val pushAlertRepository: PushAlertRepository,
    private val deviceTokenSource: DeviceTokenSource,
    private val notificationPermissionSource: NotificationPermissionSource,
    private val sealedMessageKey: SealedMessageKey,
    private val getCurrentGeohashTask: GetCurrentGeohashTask,
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
        pushAlertRepository.choices().flatMap { pushAlertChoices: PushAlertChoices ->
            if (pushAlertChoices.isAnythingChosen() && isNotificationAllowed()) {
                follow(pushAlertChoices = pushAlertChoices)
            } else {
                forget()
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
                getCurrentGeohashTask().flatMap { geohash: Geohash ->
                    pushAlertRepository.replaceSubscription(
                        installationToken = installationToken,
                        pushAlertSubscription = PushAlertSubscription(
                            geohash = geohash,
                            warningLowestLevel = pushAlertChoices.warningLowestLevel,
                            pollenChoices = pushAlertChoices.pollenChoices,
                            temperatureTrend = pushAlertChoices.temperatureTrend
                        )
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

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.pushalert")

private const val REGISTRATION_REFRESH_MILLIS = 2_592_000_000L