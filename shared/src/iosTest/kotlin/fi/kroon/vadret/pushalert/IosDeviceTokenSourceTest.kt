package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pushalert.model.DeviceToken
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class IosDeviceTokenSourceTest {
    @Test
    fun theTokenComesFromFirebase() = runTest {
        assertEquals(
            expected = DeviceToken(value = "fcm-1").asRight(),
            actual = deviceTokenSource(pushTokenProvider = FakePushTokenProvider(tokens = mutableListOf("fcm-1"))).deviceToken()
        )
    }

    @Test
    fun noTokenIsANetworkFailure() = runTest {
        assertEquals(
            expected = Failure(errorCode = ErrorCode.NETWORK_FAILURE).asLeft(),
            actual = deviceTokenSource(pushTokenProvider = FakePushTokenProvider(tokens = mutableListOf(null))).deviceToken()
        )
    }

    @Test
    fun aBuildWithoutFirebaseGetsNoToken() = runTest {
        assertEquals(
            expected = Failure(errorCode = ErrorCode.UNKNOWN).asLeft(),
            actual = IosDeviceTokenSource(pushTokenProviderHolder = PushTokenProviderHolder()).deviceToken()
        )
    }
}

private fun deviceTokenSource(pushTokenProvider: PushTokenProvider): IosDeviceTokenSource {
    val pushTokenProviderHolder = PushTokenProviderHolder()
    pushTokenProviderHolder.register(pushTokenProvider = pushTokenProvider)
    return IosDeviceTokenSource(pushTokenProviderHolder = pushTokenProviderHolder)
}