package se.vadretapp.vadret.forecastwarning.ui.map

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// The map is only made once a warning is first shown, so starting the app does not load it.
@Stable
internal class WarningMapHolder {
    var isRequested: Boolean by mutableStateOf(value = false)
    var retainedWarningMap: RetainedWarningMap? by mutableStateOf(value = null)
}