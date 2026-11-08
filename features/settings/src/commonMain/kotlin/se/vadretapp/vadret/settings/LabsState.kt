package se.vadretapp.vadret.settings

import se.vadretapp.vadret.common.Failure

internal data class LabsState(
    val isHourlyForecastEnabled: Boolean? = null,
    val failure: Failure? = null
)