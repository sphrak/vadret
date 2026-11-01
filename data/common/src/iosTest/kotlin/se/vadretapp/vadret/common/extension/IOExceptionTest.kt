package se.vadretapp.vadret.common.extension

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import se.vadretapp.vadret.common.ErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals

private const val OTHER_DOMAIN = "se.vadretapp.vadret.test"

private val urlErrorDomain: String = checkNotNull(value = NSURLErrorDomain)

class IOExceptionTest {

    private fun requestException(
        domain: String,
        code: Long
    ): DarwinHttpRequestException =
        DarwinHttpRequestException(
            origin = NSError(
                domain = domain,
                code = code,
                userInfo = null
            )
        )

    @Test
    fun `no connection or no host means the network is unavailable`() {
        assertEquals(
            expected = listOf(
                ErrorCode.NETWORK_UNAVAILABLE,
                ErrorCode.NETWORK_UNAVAILABLE,
                ErrorCode.NETWORK_UNAVAILABLE
            ),
            actual = listOf(
                NSURLErrorNotConnectedToInternet,
                NSURLErrorCannotFindHost,
                NSURLErrorDNSLookupFailed
            ).map { code: Long ->
                requestException(
                    domain = urlErrorDomain,
                    code = code
                ).networkErrorCode()
            }
        )
    }

    @Test
    fun `a dropped connection is a network failure`() {
        assertEquals(
            expected = ErrorCode.NETWORK_FAILURE,
            actual = requestException(
                domain = urlErrorDomain,
                code = NSURLErrorNetworkConnectionLost
            ).networkErrorCode()
        )
    }

    @Test
    fun `an unavailable code from another domain is a network failure`() {
        assertEquals(
            expected = ErrorCode.NETWORK_FAILURE,
            actual = requestException(
                domain = OTHER_DOMAIN,
                code = NSURLErrorNotConnectedToInternet
            ).networkErrorCode()
        )
    }
}