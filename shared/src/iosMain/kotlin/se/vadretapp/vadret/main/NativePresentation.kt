package se.vadretapp.vadret.main

import se.vadretapp.vadret.main.navigation.ScreenKind

enum class NativePresentation {
    PUSH,
    PUSH_WITHOUT_NAVIGATION_BAR,
    SHEET,
    FULL_SCREEN_COVER
}

internal fun ScreenKind.toNativePresentation(): NativePresentation =
    when (this) {
        ScreenKind.TAB -> NativePresentation.PUSH
        ScreenKind.DETAIL -> NativePresentation.PUSH
        ScreenKind.DETAIL_WITH_OWN_HEADER -> NativePresentation.PUSH_WITHOUT_NAVIGATION_BAR
        ScreenKind.SHEET -> NativePresentation.SHEET
        ScreenKind.STORY -> NativePresentation.FULL_SCREEN_COVER
    }