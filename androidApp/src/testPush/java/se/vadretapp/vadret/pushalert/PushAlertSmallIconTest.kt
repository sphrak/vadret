package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.R
import se.vadretapp.vadret.pushalert.model.PushAlertType
import kotlin.test.Test
import kotlin.test.assertEquals

class PushAlertSmallIconTest {
    @Test
    fun everyAlertTypeShowsItsOwnIconAndAnUnknownTypeTheGeneralOne() {
        assertEquals(
            expected = listOf(
                R.drawable.ic_push_alert_warning,
                R.drawable.ic_push_alert_pollen,
                R.drawable.ic_push_alert_warmer,
                R.drawable.ic_push_alert_colder,
                R.drawable.ic_push_alert_hard_wind,
                R.drawable.ic_push_alert_storm,
                R.drawable.ic_push_alert_hurricane,
                R.drawable.ic_push_alert
            ),
            actual = (PushAlertType.entries + null).map { pushAlertType -> pushAlertType.smallIconResource() }
        )
    }
}