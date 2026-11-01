package se.vadretapp.vadret.icons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
internal actual fun rememberIsMotionEnabled(): Boolean = remember { !UIAccessibilityIsReduceMotionEnabled() }