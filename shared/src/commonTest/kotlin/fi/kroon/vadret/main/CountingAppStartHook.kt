package fi.kroon.vadret.main

import fi.kroon.vadret.lifecycle.AppStartHook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class CountingAppStartHook : AppStartHook {

    val starts = MutableStateFlow(value = 0)

    override suspend fun onAppStart() {
        starts.update { started: Int -> started + 1 }
    }
}