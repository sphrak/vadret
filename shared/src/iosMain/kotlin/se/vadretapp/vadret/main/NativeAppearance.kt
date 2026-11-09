package se.vadretapp.vadret.main

import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow

class NativeAppearance internal constructor(
    private val interfaceStyle: UIUserInterfaceStyle,
    private val tabTitles: Map<NativeTab, String>
) {

    fun title(tab: NativeTab): String = tabTitles.getValue(key = tab)

    fun applyInterfaceStyle(window: UIWindow) {
        window.overrideUserInterfaceStyle = interfaceStyle
    }
}