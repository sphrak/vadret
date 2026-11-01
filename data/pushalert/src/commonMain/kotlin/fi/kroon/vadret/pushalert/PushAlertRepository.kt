package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.encrypteddatastore.encryptedDataStore
import fi.kroon.vadret.pushalert.local.AttestationLocalDataSource
import fi.kroon.vadret.pushalert.local.DefaultPushAlertLocalDataSource
import fi.kroon.vadret.pushalert.model.DeviceRegistration
import fi.kroon.vadret.pushalert.model.DeviceToken
import fi.kroon.vadret.pushalert.model.InstallationToken
import fi.kroon.vadret.pushalert.model.PushAlertChoices
import fi.kroon.vadret.pushalert.model.PushAlertStateEntity
import fi.kroon.vadret.pushalert.model.PushAlertSubscription
import fi.kroon.vadret.pushalert.remote.DefaultPushAlertRemoteDataSource
import io.github.sphrak.either.Either
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

interface PushAlertRepository {
    suspend fun installationToken(): Either<Failure, InstallationToken?>

    suspend fun install(): Either<Failure, InstallationToken>

    suspend fun registeredDevice(): Either<Failure, DeviceRegistration?>

    suspend fun registerDevice(
        installationToken: InstallationToken,
        deviceToken: DeviceToken,
        replacesDeviceToken: DeviceToken?,
        devicePublicKey: ByteArray,
        registeredAtMillis: Long
    ): Either<Failure, Unit>

    suspend fun replaceSubscription(
        installationToken: InstallationToken,
        pushAlertSubscription: PushAlertSubscription?
    ): Either<Failure, Unit>

    suspend fun forgetInstallation(installationToken: InstallationToken): Either<Failure, Unit>

    suspend fun clearInstallation(): Either<Failure, Unit>

    fun observeChoices(): Flow<Either<Failure, PushAlertChoices>>

    suspend fun choices(): Either<Failure, PushAlertChoices>

    suspend fun saveChoices(pushAlertChoices: PushAlertChoices): Either<Failure, Unit>

    fun observeIsSyncPending(): Flow<Either<Failure, Boolean>>

    suspend fun saveIsSyncPending(isSyncPending: Boolean): Either<Failure, Unit>
}

fun PushAlertRepository(
    httpClient: Lazy<HttpClient>,
    directoryPath: String,
    ioDispatcher: CoroutineDispatcher
): PushAlertRepository = DefaultPushAlertRepository(
    pushAlertRemoteDataSource = DefaultPushAlertRemoteDataSource(httpClient = httpClient),
    pushAlertLocalDataSource = DefaultPushAlertLocalDataSource(
        dataStore = encryptedDataStore(
            directoryPath = directoryPath,
            fileName = PUSH_ALERT_FILE_NAME,
            serializer = PushAlertStateEntity.serializer(),
            defaultValue = PushAlertStateEntity(),
            ioDispatcher = ioDispatcher
        )
    ),
    attestationLocalDataSource = AttestationLocalDataSource(ioDispatcher = ioDispatcher)
)

private const val PUSH_ALERT_FILE_NAME = "push_alert.json"