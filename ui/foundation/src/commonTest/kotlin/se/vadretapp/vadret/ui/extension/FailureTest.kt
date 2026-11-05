package se.vadretapp.vadret.ui.extension

import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.followed_places_limit_text
import se.vadretapp.vadret.ui.resources.unhandled_error
import kotlin.test.Test
import kotlin.test.assertEquals

class FailureTest {

    @Test
    fun `only the place limit tells about the limit`() {
        assertEquals(
            expected = mapOf(
                ErrorCode.FOLLOWED_PLACE_LIMIT to Res.string.followed_places_limit_text,
                ErrorCode.PUSH_ALERT_NOT_CHOSEN to Res.string.unhandled_error,
                ErrorCode.FAVORITE_NAME_TAKEN to Res.string.unhandled_error
            ),
            actual = listOf(
                ErrorCode.FOLLOWED_PLACE_LIMIT,
                ErrorCode.PUSH_ALERT_NOT_CHOSEN,
                ErrorCode.FAVORITE_NAME_TAKEN
            ).associateWith { errorCode: ErrorCode -> Failure(errorCode = errorCode).messageResource() }
        )
    }
}