package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.icons.Colder
import se.vadretapp.vadret.icons.HardWind
import se.vadretapp.vadret.icons.Hurricane
import se.vadretapp.vadret.icons.Pollen
import se.vadretapp.vadret.icons.Slipperiness
import se.vadretapp.vadret.icons.Storm
import se.vadretapp.vadret.icons.VadretIcons
import se.vadretapp.vadret.icons.Warmer
import se.vadretapp.vadret.icons.Warning
import se.vadretapp.vadret.pushalert.extension.icon
import se.vadretapp.vadret.pushalert.model.PushAlertType
import kotlin.test.Test
import kotlin.test.assertEquals

class PushAlertTypeIconTest {
    @Test
    fun everyAlertTypeShowsItsOwnIcon() {
        assertEquals(
            expected = listOf(
                VadretIcons.Alert.Warning,
                VadretIcons.Alert.Pollen,
                VadretIcons.Alert.Warmer,
                VadretIcons.Alert.Colder,
                VadretIcons.Alert.HardWind,
                VadretIcons.Alert.Storm,
                VadretIcons.Alert.Hurricane,
                VadretIcons.Alert.Slipperiness
            ),
            actual = PushAlertType.entries.map { pushAlertType: PushAlertType -> pushAlertType.icon() }
        )
    }
}