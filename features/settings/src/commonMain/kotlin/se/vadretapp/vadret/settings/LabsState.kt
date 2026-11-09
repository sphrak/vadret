package se.vadretapp.vadret.settings

import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.settings.model.Labs

internal data class LabsState(
    val labs: Labs? = null,
    val isNativeNavigationEnabled: Boolean? = null,
    val failure: Failure? = null
)