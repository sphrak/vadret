package fi.kroon.vadret.pushalert

internal class FakePushTokenProvider(
    private val tokens: MutableList<String?>
) : PushTokenProvider {
    override fun fetchToken(completion: (String?, String?) -> Unit) {
        val token = tokens.removeAt(index = 0)
        if (token == null) {
            completion(null, "no network")
        } else {
            completion(token, null)
        }
    }
}