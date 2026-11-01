package se.vadretapp.vadret.common.extension

import io.ktor.client.call.body
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.mockHttpClient
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val TAG = "se.vadretapp.vadret.common.test"

@Serializable
private data class Expected(val value: String)

class ExceptionTest {

    private val logger: Logger = LoggerFactory.getLogger(tag = TAG)

    private suspend fun responseException(statusCode: Int): Exception {
        val httpClient = mockHttpClient {
            respond(
                content = "",
                status = HttpStatusCode.fromValue(value = statusCode)
            )
        }
        return try {
            httpClient.get(urlString = "https://example.com") { expectSuccess = true }
            error("Expected $statusCode to throw")
        } catch (exception: Exception) {
            exception
        }
    }

    @Test
    fun `http statuses map to their error codes`() =
        runTest {
            val expected: Map<Int, ErrorCode> = mapOf(
                400 to ErrorCode.HTTP_BAD_REQUEST,
                401 to ErrorCode.HTTP_UNAUTHORIZED,
                403 to ErrorCode.HTTP_FORBIDDEN,
                404 to ErrorCode.HTTP_NOT_FOUND,
                418 to ErrorCode.HTTP_UNEXPECTED_STATUS,
                500 to ErrorCode.HTTP_INTERNAL_SERVER_ERROR,
                502 to ErrorCode.HTTP_UNEXPECTED_STATUS,
                503 to ErrorCode.HTTP_SERVICE_UNAVAILABLE,
                504 to ErrorCode.HTTP_GATEWAY_TIMEOUT
            )

            val actual: Map<Int, ErrorCode?> = expected.keys.associateWith { statusCode: Int ->
                responseException(statusCode = statusCode).networkErrorCode()
            }

            assertEquals(expected = expected, actual = actual)
        }

    @Test
    fun `other input and output failures are network failures`() {
        assertEquals(
            expected = ErrorCode.NETWORK_FAILURE,
            actual = IOException("reset").networkErrorCode()
        )
    }

    @Test
    fun `an unreadable body is an invalid response`() {
        assertEquals(
            expected = ErrorCode.INVALID_RESPONSE,
            actual = SerializationException("missing field").networkErrorCode()
        )
    }

    @Test
    fun `a body that is not the json expected is an invalid response`() =
        runTest {
            val bodies: Map<String, Map<String, String>> = mapOf(
                """{"unexpected":true}""" to mapOf(HttpHeaders.ContentType to "application/json"),
                "<html>Log in to the wifi</html>" to mapOf(HttpHeaders.ContentType to "text/html")
            )

            val actual: List<ErrorCode?> = bodies.map { (body: String, headers: Map<String, String>) ->
                val httpClient = mockHttpClient {
                    respond(
                        content = body,
                        headers = headersOf(HttpHeaders.ContentType, headers.getValue(HttpHeaders.ContentType))
                    )
                }
                val exception: Exception = try {
                    httpClient.get(urlString = "https://example.com").body<Expected>()
                    error("Expected $body to throw")
                } catch (exception: Exception) {
                    exception
                }
                exception.networkErrorCode()
            }

            assertEquals(
                expected = listOf(ErrorCode.INVALID_RESPONSE, ErrorCode.INVALID_RESPONSE),
                actual = actual
            )
        }

    @Test
    fun `anything else is not a network failure`() {
        assertNull(actual = IllegalStateException("bug").networkErrorCode())
    }

    @Test
    fun `a known failure logs a warning without the stack trace`() {
        withRecordedLogs { recordingLogSink ->
            val failure: Failure = IOException("reset").toKnownFailure(
                errorCode = ErrorCode.NETWORK_FAILURE,
                logger = logger
            )

            assertEquals(expected = Failure(errorCode = ErrorCode.NETWORK_FAILURE), actual = failure)
            assertEquals(
                expected = listOf(
                    LogLine(
                        level = LogLevel.WARN,
                        tag = TAG,
                        message = "NETWORK_FAILURE: IOException: reset"
                    )
                ),
                actual = recordingLogSink.lines
            )
        }
    }

    @Test
    fun `an unknown failure logs an error with the stack trace`() {
        withRecordedLogs { recordingLogSink ->
            val failure: Failure = IllegalStateException("bug").toUnknownFailure(logger = logger)

            assertEquals(expected = Failure.unknown(), actual = failure)
            val line: LogLine = recordingLogSink.lines.single()
            assertEquals(expected = LogLevel.ERROR, actual = line.level)
            assertTrue(actual = line.message.contains("IllegalStateException: bug"))
            assertTrue(actual = line.message.contains("ExceptionTest"))
        }
    }

    @Test
    fun `a network failure is known and anything else unknown`() {
        withRecordedLogs { recordingLogSink ->
            val failures: List<Failure> = listOf(
                IOException("reset").toNetworkFailure(logger = logger),
                IllegalStateException("bug").toNetworkFailure(logger = logger)
            )

            assertEquals(
                expected = listOf(
                    Failure(errorCode = ErrorCode.NETWORK_FAILURE),
                    Failure.unknown()
                ),
                actual = failures
            )
            assertEquals(
                expected = listOf(LogLevel.WARN, LogLevel.ERROR),
                actual = recordingLogSink.lines.map { line: LogLine -> line.level }
            )
        }
    }
}