package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals

class PushAlertsAppStartHookTest {
    @Test
    fun aSyncThatFailsAtStartIsLoggedAsAWarning() = runTest {
        withRecordedLogs { recordingLogSink ->
            PushAlertsAppStartHook(
                syncPushAlertsTask = FakeSyncPushAlertsTask(result = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft())
            ).onAppStart()

            assertEquals(
                expected = listOf(LogLevel.WARN to "Push alerts could not be brought up to date: NETWORK_UNAVAILABLE"),
                actual = recordingLogSink.lines.map { line -> line.level to line.message }
            )
        }
    }

    @Test
    fun aSyncThatWorksAtStartIsOnlyTraced() = runTest {
        withRecordedLogs { recordingLogSink ->
            PushAlertsAppStartHook(syncPushAlertsTask = FakeSyncPushAlertsTask(result = Unit.asRight())).onAppStart()

            assertEquals(
                expected = listOf(LogLevel.DEBUG),
                actual = recordingLogSink.lines.map { line -> line.level }
            )
        }
    }
}