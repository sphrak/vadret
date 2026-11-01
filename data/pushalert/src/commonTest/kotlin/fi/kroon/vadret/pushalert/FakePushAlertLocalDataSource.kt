package fi.kroon.vadret.pushalert

import fi.kroon.vadret.pushalert.error.StorageException
import fi.kroon.vadret.pushalert.local.PushAlertLocalDataSource
import fi.kroon.vadret.pushalert.model.PushAlertStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

internal class FakePushAlertLocalDataSource(
    initialState: PushAlertStateEntity = PushAlertStateEntity(),
    private val isBroken: Boolean = false
) : PushAlertLocalDataSource {
    val state = MutableStateFlow(value = initialState)

    override fun observeState(): Flow<PushAlertStateEntity> = if (isBroken) {
        flow { throw StorageException(message = "broken") }
    } else {
        state
    }

    override suspend fun state(): PushAlertStateEntity {
        if (isBroken) {
            throw StorageException(message = "broken")
        }
        return state.value
    }

    override suspend fun updateState(transform: (PushAlertStateEntity) -> PushAlertStateEntity) {
        if (isBroken) {
            throw StorageException(message = "broken")
        }
        state.update(function = transform)
    }
}