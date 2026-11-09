package se.vadretapp.vadret.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

val LocalNativeNavigationBar: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }