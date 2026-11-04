package se.vadretapp.vadret.lifecycle

interface AppStartHook {
    suspend fun onAppStart()
}