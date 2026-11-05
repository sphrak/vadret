package se.vadretapp.vadret.settings.extension

import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.settings.model.FailureDialog
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.followed_places_limit
import se.vadretapp.vadret.ui.resources.followed_places_limit_text
import se.vadretapp.vadret.ui.resources.no_location_permission
import se.vadretapp.vadret.ui.resources.settings_places
import kotlin.test.Test
import kotlin.test.assertEquals

class FailureTest {

    @Test
    fun `the place limit has a title of its own and other failures are titled by the screen`() {
        assertEquals(
            expected = listOf(
                FailureDialog(
                    titleResource = Res.string.followed_places_limit,
                    textResource = Res.string.followed_places_limit_text
                ),
                FailureDialog(
                    titleResource = Res.string.settings_places,
                    textResource = Res.string.no_location_permission
                )
            ),
            actual = listOf(
                Failure(errorCode = ErrorCode.FOLLOWED_PLACE_LIMIT),
                Failure(errorCode = ErrorCode.LOCATION_PERMISSION_MISSING)
            ).map { failure: Failure -> failure.toPlacesFailureDialog() }
        )
    }
}