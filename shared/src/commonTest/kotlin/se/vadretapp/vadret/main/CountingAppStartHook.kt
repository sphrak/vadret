package se.vadretapp.vadret.main

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import se.vadretapp.vadret.lifecycle.AppStartHook

internal class CountingAppStartHook : AppStartHook {

    val starts = MutableStateFlow(value = 0)

    override suspend fun onAppStart() {
        starts.update { started: Int -> started + 1 }
    }
}