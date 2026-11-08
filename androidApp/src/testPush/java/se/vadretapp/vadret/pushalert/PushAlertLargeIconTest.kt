package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.R
import se.vadretapp.vadret.pushalert.model.PushAlertType
import kotlin.test.Test
import kotlin.test.assertEquals

class PushAlertLargeIconTest {
    @Test
    fun everyAlertTypeShowsItsOwnLargeIcon() {
        assertEquals(
            expected = listOf(
                R.drawable.ic_push_alert_warning,
                R.drawable.ic_push_alert_pollen,
                R.drawable.ic_push_alert_warmer,
                R.drawable.ic_push_alert_colder,
                R.drawable.ic_push_alert_hard_wind,
                R.drawable.ic_push_alert_storm,
                R.drawable.ic_push_alert_hurricane,
                R.drawable.ic_push_alert_slipperiness
            ),
            actual = PushAlertType.entries.map { pushAlertType: PushAlertType -> pushAlertType.largeIconResource() }
        )
    }

    @Test
    fun windAlertsShareOneColourAndEveryOtherTypeHasItsOwn() {
        assertEquals(
            expected = listOf(
                R.color.push_alert_warning,
                R.color.push_alert_pollen,
                R.color.push_alert_warmer,
                R.color.push_alert_colder,
                R.color.push_alert_wind,
                R.color.push_alert_wind,
                R.color.push_alert_wind,
                R.color.push_alert_slipperiness
            ),
            actual = PushAlertType.entries.map { pushAlertType: PushAlertType -> pushAlertType.largeIconColorResource() }
        )
    }
}