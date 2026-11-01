package fi.kroon.vadret.pushalert

interface PushTokenProvider {
    fun fetchToken(completion: (String?, String?) -> Unit)
}