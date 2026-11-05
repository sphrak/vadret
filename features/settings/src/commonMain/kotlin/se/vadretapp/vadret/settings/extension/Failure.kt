package se.vadretapp.vadret.settings.extension

import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.settings.model.FailureDialog
import se.vadretapp.vadret.ui.extension.messageResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.followed_places_limit
import se.vadretapp.vadret.ui.resources.settings_places

internal fun Failure.toPlacesFailureDialog(): FailureDialog =
    FailureDialog(
        titleResource = if (errorCode == ErrorCode.FOLLOWED_PLACE_LIMIT) {
            Res.string.followed_places_limit
        } else {
            Res.string.settings_places
        },
        textResource = messageResource()
    )