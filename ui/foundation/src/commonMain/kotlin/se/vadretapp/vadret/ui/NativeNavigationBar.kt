package se.vadretapp.vadret.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalNativeNavigationBar: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

val LocalScrollBottomInset: ProvidableCompositionLocal<Dp> = compositionLocalOf { 0.dp }