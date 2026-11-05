package se.vadretapp.vadret.settings.extension

import kotlinx.collections.immutable.persistentListOf
import se.vadretapp.vadret.followedplace.model.LocationChoices
import se.vadretapp.vadret.settings.model.RowSummary
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.followed_place_position
import kotlin.test.Test
import kotlin.test.assertEquals

class LocationChoicesTest {

    @Test
    fun `the places row names the position while it is followed and else the chosen place`() {
        assertEquals(
            expected = listOf(
                RowSummary.Resource(resource = Res.string.followed_place_position),
                RowSummary.Plain(text = "Umeå"),
                null
            ),
            actual = listOf(
                LocationChoices(
                    isAutomatic = true,
                    chosenPlaceName = null,
                    favorites = persistentListOf()
                ),
                LocationChoices(
                    isAutomatic = false,
                    chosenPlaceName = "Umeå",
                    favorites = persistentListOf()
                ),
                LocationChoices(
                    isAutomatic = false,
                    chosenPlaceName = null,
                    favorites = persistentListOf()
                )
            ).map { locationChoices: LocationChoices -> locationChoices.toPlacesSummary() }
        )
    }
}