package se.vadretapp.vadret.main.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenKindTest {

    @Test
    fun `the tab roots are tabs`() {
        val tabKeys: List<VadretKey> = TopLevelDestination.entries.map { destination: TopLevelDestination -> destination.key }

        assertEquals(
            expected = tabKeys.map { ScreenKind.TAB },
            actual = tabKeys.map { key: VadretKey -> key.screenKind }
        )
    }

    @Test
    fun `a place forecast and a warning draw their own header`() {
        val keys: List<VadretKey> = listOf(
            PlaceForecastKey(
                latitude = 59.33,
                longitude = 18.07
            ),
            WarningDetailKey(warningIdentifier = "smhi-1-1")
        )

        assertEquals(
            expected = keys.map { ScreenKind.DETAIL_WITH_OWN_HEADER },
            actual = keys.map { key: VadretKey -> key.screenKind }
        )
    }

    @Test
    fun `the followed places and the place search are sheets`() {
        val keys: List<VadretKey> = listOf(
            WarningPlacesKey,
            PlaceSearchKey
        )

        assertEquals(
            expected = keys.map { ScreenKind.SHEET },
            actual = keys.map { key: VadretKey -> key.screenKind }
        )
    }

    @Test
    fun `a warning story and a simulated one are stories`() {
        val keys: List<VadretKey> = listOf(
            WarningStoryKey(warningIdentifiers = listOf("smhi-1-1")),
            SimulatedWarningStoryKey(warningIdentifiers = listOf("smhi-1-1"))
        )

        assertEquals(
            expected = keys.map { ScreenKind.STORY },
            actual = keys.map { key: VadretKey -> key.screenKind }
        )
    }

    @Test
    fun `the settings screens are details`() {
        val keys: List<VadretKey> = listOf(
            SettingsPlacesKey,
            SettingsSectionKey(sectionKey = "notifications"),
            SettingsPollenKey,
            SettingsLabsKey,
            SettingsAboutKey,
            SettingsLicensesKey,
            SettingsDeveloperKey,
            SettingsDeveloperSectionKey(sectionKey = "simulation")
        )

        assertEquals(
            expected = keys.map { ScreenKind.DETAIL },
            actual = keys.map { key: VadretKey -> key.screenKind }
        )
    }
}