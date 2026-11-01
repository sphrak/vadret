package fi.kroon.vadret.pushalert.local

import androidx.datastore.core.DataStore
import fi.kroon.vadret.pushalert.error.StorageException
import fi.kroon.vadret.pushalert.model.PushAlertStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import okio.IOException

internal class DefaultPushAlertLocalDataSource(
    private val dataStore: DataStore<PushAlertStateEntity>
) : PushAlertLocalDataSource {
    override fun observeState(): Flow<PushAlertStateEntity> =
        dataStore.data.catch { exception: Throwable ->
            if (exception is IOException) {
                throw StorageException(message = "The push alert state cannot be read: ${exception.message}")
            }
            throw exception
        }

    override suspend fun state(): PushAlertStateEntity =
        try {
            dataStore.data.first()
        } catch (exception: IOException) {
            throw StorageException(message = "The push alert state cannot be read: ${exception.message}")
        }

    override suspend fun updateState(transform: (PushAlertStateEntity) -> PushAlertStateEntity) {
        try {
            dataStore.updateData { pushAlertStateEntity -> transform(pushAlertStateEntity) }
        } catch (exception: IOException) {
            throw StorageException(message = "The push alert state cannot be written: ${exception.message}")
        }
    }
}