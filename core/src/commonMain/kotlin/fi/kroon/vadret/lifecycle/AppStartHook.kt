package fi.kroon.vadret.lifecycle

interface AppStartHook {
    suspend fun onAppStart()
}