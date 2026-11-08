package se.vadretapp.vadret.ui.model

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.back
import se.vadretapp.vadret.ui.resources.close
import se.vadretapp.vadret.ui.resources.ic_arrow_back
import se.vadretapp.vadret.ui.resources.ic_close

enum class NavigationIcon {
    BACK,
    CLOSE
}

internal fun NavigationIcon.iconResource(): DrawableResource =
    when (this) {
        NavigationIcon.BACK -> Res.drawable.ic_arrow_back
        NavigationIcon.CLOSE -> Res.drawable.ic_close
    }

internal fun NavigationIcon.descriptionResource(): StringResource =
    when (this) {
        NavigationIcon.BACK -> Res.string.back
        NavigationIcon.CLOSE -> Res.string.close
    }