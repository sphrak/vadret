package se.vadretapp.vadret.pushalert

import kotlinx.coroutines.flow.MutableStateFlow

internal class PushTokenProviderHolder {
    private val registeredPushTokenProvider = MutableStateFlow<PushTokenProvider?>(value = null)

    fun register(pushTokenProvider: PushTokenProvider) {
        registeredPushTokenProvider.value = pushTokenProvider
    }

    fun pushTokenProvider(): PushTokenProvider? = registeredPushTokenProvider.value
}