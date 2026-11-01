package se.vadretapp.vadret.pushalert.local

import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.pushalert.model.PushAlertStateEntity

internal interface PushAlertLocalDataSource {
    fun observeState(): Flow<PushAlertStateEntity>

    suspend fun state(): PushAlertStateEntity

    suspend fun updateState(transform: (PushAlertStateEntity) -> PushAlertStateEntity)
}