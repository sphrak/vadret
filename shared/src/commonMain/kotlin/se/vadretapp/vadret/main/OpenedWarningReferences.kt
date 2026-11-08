package se.vadretapp.vadret.main

import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference

interface OpenedWarningReferences {

    val warningReferences: Flow<WarningReference>

    fun open(warningReference: WarningReference)
}

fun OpenedWarningReferences(): OpenedWarningReferences = DefaultOpenedWarningReferences()