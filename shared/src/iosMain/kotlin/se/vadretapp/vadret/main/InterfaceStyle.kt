package se.vadretapp.vadret.main

import platform.UIKit.UIUserInterfaceStyle
import se.vadretapp.vadret.setting.model.Theme

// Following the system leaves the style unset, since an override would also hide the system's own switches
// from the app.
internal fun Theme.toInterfaceStyle(): UIUserInterfaceStyle =
    when (this) {
        Theme.LIGHT -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
        Theme.DARK -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        Theme.AMOLED -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        Theme.SYSTEM -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
    }