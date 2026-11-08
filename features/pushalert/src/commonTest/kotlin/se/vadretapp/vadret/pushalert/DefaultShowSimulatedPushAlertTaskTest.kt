package se.vadretapp.vadret.pushalert

import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.usecase.DefaultShowSimulatedPushAlertTask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DefaultShowSimulatedPushAlertTaskTest {
    @Test
    fun anAlertIsShownWhenNotificationsAreAllowed() = runTest {
        val pushAlertNotifier = FakePushAlertNotifier()
        val showSimulatedPushAlertTask = DefaultShowSimulatedPushAlertTask(
            notificationPermissionSource = FakeNotificationPermissionSource(isNotificationAllowed = true),
            pushAlertNotifier = pushAlertNotifier
        )

        val isShown: Boolean = showSimulatedPushAlertTask(pushAlertMessage = WARNING_MESSAGE)

        assertEquals(
            expected = true to listOf(WARNING_MESSAGE),
            actual = isShown to pushAlertNotifier.shown
        )
    }

    @Test
    fun nothingIsShownWhenNotificationsAreOff() = runTest {
        val pushAlertNotifier = FakePushAlertNotifier()
        val showSimulatedPushAlertTask = DefaultShowSimulatedPushAlertTask(
            notificationPermissionSource = FakeNotificationPermissionSource(isNotificationAllowed = false),
            pushAlertNotifier = pushAlertNotifier
        )

        val isShown: Boolean = showSimulatedPushAlertTask(pushAlertMessage = WARNING_MESSAGE)

        assertEquals(
            expected = false to emptyList<PushAlertMessage>(),
            actual = isShown to pushAlertNotifier.shown
        )
    }
}

private val WARNING_MESSAGE: PushAlertMessage = simulatedPushAlertMessages(now = Instant.parse(input = "2026-11-08T18:00:00Z")).first()