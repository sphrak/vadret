package se.vadretapp.vadret.settings.extension

import se.vadretapp.vadret.followedplace.model.LocationChoices
import se.vadretapp.vadret.settings.model.RowSummary
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.followed_place_position

// The places row names the place the forecast opens on, which is the position while it is followed.
internal fun LocationChoices.toPlacesSummary(): RowSummary? {
    val placeName: String? = chosenPlaceName
    return if (isAutomatic) {
        RowSummary.Resource(resource = Res.string.followed_place_position)
    } else if (placeName != null) {
        RowSummary.Plain(text = placeName)
    } else {
        null
    }
}