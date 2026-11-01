package se.vadretapp.vadret.pushalert

interface PushTokenProvider {
    fun fetchToken(completion: (String?, String?) -> Unit)
}