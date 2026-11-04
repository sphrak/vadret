package se.vadretapp.vadret.radar.extension

import kotlinx.io.IOException
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.radar.exception.TiffFormatException
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ThrowableTest {

    @Test
    fun `a cancellation is thrown again`() {
        val throwable: Throwable = CancellationException("cancelled")

        assertFailsWith<CancellationException> { throwable.toFailure() }
    }

    @Test
    fun `known failures get their error code and a warning`() {
        val knownFailures: List<Pair<Throwable, ErrorCode>> = listOf(
            TiffFormatException(message = "The byte order mark is missing") to ErrorCode.INVALID_RESPONSE,
            IOException("Connection reset") to ErrorCode.NETWORK_FAILURE
        )

        withRecordedLogs { recordingLogSink ->
            assertEquals(
                expected = knownFailures.map { (_, errorCode: ErrorCode) -> Failure(errorCode = errorCode) },
                actual = knownFailures.map { (throwable: Throwable, _) -> throwable.toFailure() }
            )
            assertEquals(
                expected = List(size = knownFailures.size) { LogLevel.WARN },
                actual = recordingLogSink.lines.map { line: LogLine -> line.level }
            )
        }
    }

    @Test
    fun `unknown failures are unknown and logged as errors`() {
        val unknownFailures: List<Throwable> = listOf(
            IllegalStateException("Unable to create a decoder"),
            OutOfMemoryError("Bitmap too large")
        )

        withRecordedLogs { recordingLogSink ->
            assertEquals(
                expected = List(size = unknownFailures.size) { Failure.unknown() },
                actual = unknownFailures.map { throwable: Throwable -> throwable.toFailure() }
            )
            assertEquals(
                expected = List(size = unknownFailures.size) { LogLevel.ERROR },
                actual = recordingLogSink.lines.map { line: LogLine -> line.level }
            )
        }
    }
}