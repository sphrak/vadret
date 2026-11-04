package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.pushalert.local.AttestationLocalDataSource
import se.vadretapp.vadret.pushalert.local.DefaultPushAlertLocalDataSource
import se.vadretapp.vadret.pushalert.local.encryptedDataStore
import se.vadretapp.vadret.pushalert.model.DeviceRegistration
import se.vadretapp.vadret.pushalert.model.DeviceToken
import se.vadretapp.vadret.pushalert.model.InstallationToken
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertStateEntity
import se.vadretapp.vadret.pushalert.model.PushAlertSubscription
import se.vadretapp.vadret.pushalert.remote.DefaultPushAlertRemoteDataSource

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

    suspend fun syncedCurrentGeohash(): Either<Failure, Geohash?>

    suspend fun saveSyncedCurrentGeohash(geohash: Geohash): Either<Failure, Unit>

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