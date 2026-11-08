package se.vadretapp.vadret.main

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference

internal class DefaultOpenedWarningReferences : OpenedWarningReferences {

    private val openedWarningReferences: Channel<WarningReference> = Channel(capacity = Channel.CONFLATED)

    override val warningReferences: Flow<WarningReference> = openedWarningReferences.receiveAsFlow()

    override fun open(warningReference: WarningReference) {
        openedWarningReferences.trySend(element = warningReference)
    }
}