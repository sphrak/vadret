package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.common.catch
import fi.kroon.vadret.common.either
import fi.kroon.vadret.pushalert.extension.toChallenge
import fi.kroon.vadret.pushalert.extension.toDeviceRegistration
import fi.kroon.vadret.pushalert.extension.toDeviceRequestDto
import fi.kroon.vadret.pushalert.extension.toFailure
import fi.kroon.vadret.pushalert.extension.toInstallationRequestDto
import fi.kroon.vadret.pushalert.extension.toInstallationToken
import fi.kroon.vadret.pushalert.extension.toPushAlertChoices
import fi.kroon.vadret.pushalert.extension.toPushAlertChoicesEntity
import fi.kroon.vadret.pushalert.extension.toSubscriptionDto
import fi.kroon.vadret.pushalert.extension.withDeviceRegistration
import fi.kroon.vadret.pushalert.extension.withInstallationToken
import fi.kroon.vadret.pushalert.extension.withoutInstallation
import fi.kroon.vadret.pushalert.local.AttestationLocalDataSource
import fi.kroon.vadret.pushalert.local.PushAlertLocalDataSource
import fi.kroon.vadret.pushalert.model.DeviceRegistration
import fi.kroon.vadret.pushalert.model.DeviceToken
import fi.kroon.vadret.pushalert.model.InstallationToken
import fi.kroon.vadret.pushalert.model.PushAlertChoices
import fi.kroon.vadret.pushalert.model.PushAlertStateEntity
import fi.kroon.vadret.pushalert.model.PushAlertSubscription
import fi.kroon.vadret.pushalert.remote.PushAlertRemoteDataSource
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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