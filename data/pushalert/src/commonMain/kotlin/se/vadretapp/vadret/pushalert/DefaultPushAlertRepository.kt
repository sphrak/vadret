package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.pushalert.extension.toChallenge
import se.vadretapp.vadret.pushalert.extension.toDeviceRegistration
import se.vadretapp.vadret.pushalert.extension.toDeviceRequestDto
import se.vadretapp.vadret.pushalert.extension.toFailure
import se.vadretapp.vadret.pushalert.extension.toInstallationRequestDto
import se.vadretapp.vadret.pushalert.extension.toInstallationToken
import se.vadretapp.vadret.pushalert.extension.toPushAlertChoices
import se.vadretapp.vadret.pushalert.extension.toPushAlertChoicesEntity
import se.vadretapp.vadret.pushalert.extension.toSubscriptionDto
import se.vadretapp.vadret.pushalert.extension.toSyncedCurrentGeohash
import se.vadretapp.vadret.pushalert.extension.withDeviceRegistration
import se.vadretapp.vadret.pushalert.extension.withInstallationToken
import se.vadretapp.vadret.pushalert.extension.withoutInstallation
import se.vadretapp.vadret.pushalert.local.AttestationLocalDataSource
import se.vadretapp.vadret.pushalert.local.PushAlertLocalDataSource
import se.vadretapp.vadret.pushalert.model.DeviceRegistration
import se.vadretapp.vadret.pushalert.model.DeviceToken
import se.vadretapp.vadret.pushalert.model.InstallationToken
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertStateEntity
import se.vadretapp.vadret.pushalert.model.PushAlertSubscription
import se.vadretapp.vadret.pushalert.remote.PushAlertRemoteDataSource
import kotlin.io.encoding.Base64

internal class DefaultPushAlertRepository(
    private val pushAlertRemoteDataSource: PushAlertRemoteDataSource,
    private val pushAlertLocalDataSource: PushAlertLocalDataSource,
    private val attestationLocalDataSource: AttestationLocalDataSource
) : PushAlertRepository {
    override suspend fun installationToken(): Either<Failure, InstallationToken?> =
        either {
            pushAlertLocalDataSource.state().toInstallationToken()
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun install(): Either<Failure, InstallationToken> =
        either {
            val challenge = pushAlertRemoteDataSource.createChallenge().toChallenge()
            val attestation = attestationLocalDataSource.attest(challenge = Base64.decode(source = challenge))
            val installationRequestDto = attestation.toInstallationRequestDto(challenge = challenge)
            val installationToken = pushAlertRemoteDataSource.install(installationRequestDto = installationRequestDto).toInstallationToken()
            pushAlertLocalDataSource.updateState { pushAlertStateEntity -> pushAlertStateEntity.withInstallationToken(installationToken = installationToken) }
            installationToken
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun registeredDevice(): Either<Failure, DeviceRegistration?> =
        either {
            pushAlertLocalDataSource.state().toDeviceRegistration()
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun registerDevice(
        installationToken: InstallationToken,
        deviceToken: DeviceToken,
        replacesDeviceToken: DeviceToken?,
        devicePublicKey: ByteArray,
        registeredAtMillis: Long
    ): Either<Failure, Unit> =
        either {
            val deviceRegistration = DeviceRegistration(
                deviceToken = deviceToken,
                devicePublicKey = Base64.encode(source = devicePublicKey),
                registeredAtMillis = registeredAtMillis
            )
            pushAlertRemoteDataSource.registerDevice(
                installationToken = installationToken.value,
                deviceRequestDto = deviceRegistration.toDeviceRequestDto(replacesDeviceToken = replacesDeviceToken)
            )
            pushAlertLocalDataSource.updateState { pushAlertStateEntity -> pushAlertStateEntity.withDeviceRegistration(deviceRegistration = deviceRegistration) }
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun replaceSubscription(
        installationToken: InstallationToken,
        pushAlertSubscription: PushAlertSubscription?
    ): Either<Failure, Unit> =
        either {
            pushAlertRemoteDataSource.replaceSubscription(
                installationToken = installationToken.value,
                subscriptionDto = pushAlertSubscription.toSubscriptionDto()
            )
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun syncedCurrentGeohash(): Either<Failure, Geohash?> =
        either {
            pushAlertLocalDataSource.state().toSyncedCurrentGeohash()
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun saveSyncedCurrentGeohash(geohash: Geohash): Either<Failure, Unit> =
        either {
            pushAlertLocalDataSource.updateState { pushAlertStateEntity -> pushAlertStateEntity.copy(syncedCurrentGeohash = geohash.value) }
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun forgetInstallation(installationToken: InstallationToken): Either<Failure, Unit> =
        either {
            pushAlertRemoteDataSource.deleteInstallation(installationToken = installationToken.value)
            pushAlertLocalDataSource.updateState { pushAlertStateEntity -> pushAlertStateEntity.withoutInstallation() }
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun clearInstallation(): Either<Failure, Unit> =
        either {
            pushAlertLocalDataSource.updateState { pushAlertStateEntity -> pushAlertStateEntity.withoutInstallation() }
        }.catch { exception: Exception -> exception.toFailure() }

    override fun observeChoices(): Flow<Either<Failure, PushAlertChoices>> =
        observeState { pushAlertStateEntity -> pushAlertStateEntity.choices.toPushAlertChoices() }

    override suspend fun choices(): Either<Failure, PushAlertChoices> =
        either {
            pushAlertLocalDataSource.state().choices.toPushAlertChoices()
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun saveChoices(pushAlertChoices: PushAlertChoices): Either<Failure, Unit> =
        either {
            pushAlertLocalDataSource.updateState { pushAlertStateEntity -> pushAlertStateEntity.copy(choices = pushAlertChoices.toPushAlertChoicesEntity()) }
        }.catch { exception: Exception -> exception.toFailure() }

    override fun observeIsSyncPending(): Flow<Either<Failure, Boolean>> =
        observeState { pushAlertStateEntity -> pushAlertStateEntity.isSyncPending }

    override suspend fun saveIsSyncPending(isSyncPending: Boolean): Either<Failure, Unit> =
        either {
            pushAlertLocalDataSource.updateState { pushAlertStateEntity -> pushAlertStateEntity.copy(isSyncPending = isSyncPending) }
        }.catch { exception: Exception -> exception.toFailure() }

    private fun <T> observeState(transform: (PushAlertStateEntity) -> T): Flow<Either<Failure, T>> =
        pushAlertLocalDataSource.observeState()
            .map<PushAlertStateEntity, Either<Failure, T>> { pushAlertStateEntity -> transform(pushAlertStateEntity).asRight() }
            .distinctUntilChanged()
            .catch { exception: Throwable ->
                if (exception is Exception) {
                    emit(value = exception.toFailure().asLeft())
                } else {
                    throw exception
                }
            }
}