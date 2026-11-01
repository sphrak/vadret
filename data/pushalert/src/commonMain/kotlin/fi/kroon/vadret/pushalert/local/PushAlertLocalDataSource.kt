package fi.kroon.vadret.pushalert.local

import fi.kroon.vadret.pushalert.model.PushAlertStateEntity
import kotlinx.coroutines.flow.Flow

internal interface PushAlertLocalDataSource {
    fun observeState(): Flow<PushAlertStateEntity>

    suspend fun state(): PushAlertStateEntity

    suspend fun updateState(transform: (PushAlertStateEntity) -> PushAlertStateEntity)
}