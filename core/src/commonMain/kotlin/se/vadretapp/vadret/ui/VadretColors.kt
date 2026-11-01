package se.vadretapp.vadret.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class VadretColors(
    val divider: Color,
    val icon: Color,
    val precipitation: Color,
    val accent: Color = lightAccent,
    val temperatureCold: Color = Color(color = 0xFF5AA9D6),
    val temperatureCool: Color = Color(color = 0xFF7F8EEE),
    val temperatureMild: Color = Color(color = 0xFFA58AD8),
    val temperatureWarm: Color = Color(color = 0xFFE8A35A),
    val warningMessage: Color = Color(color = 0xFFDFE3F3),
    val onWarningMessage: Color = Color(color = 0xFF263056),
    val warningYellow: Color = Color(color = 0xFFFFE27A),
    val onWarningYellow: Color = Color(color = 0xFF3D3000),
    val warningOrange: Color = Color(color = 0xFFFFB35C),
    val onWarningOrange: Color = Color(color = 0xFF3D1F00),
    val warningRed: Color = Color(color = 0xFFE5484D),
    val onWarningRed: Color = Color.White,
    val districtLand: Color = Color(color = 0xFFCDE7CA),
    val districtLandStroke: Color = Color(color = 0xFFB1C9AD),
    val districtSea: Color = Color(color = 0xFFD2DFF8),
    val districtSeaStroke: Color = Color(color = 0xFFBBC9E6),
    val krisinformation: Color = Color(color = 0xFFD2DFF8),
    val krisinformationStroke: Color = Color(color = 0xFFBBC9E6),
    val smhi: Color = Color(color = 0xFFCDE7CA),
    val smhiStroke: Color = Color(color = 0xFFB1C9AD),
    val unknownSource: Color = Color(color = 0xFFD0C6DF),
    val chipText: Color = Color(color = 0xFF2B2B2B)
)

// The light message pale lavender glares on a dark screen, so dark themes use a dim indigo with light text (8.7:1).
private val darkWarningMessage = Color(color = 0xFF283154)
private val onDarkWarningMessage = Color(color = 0xFFCDD5F5)

internal val lightVadretColors: VadretColors = VadretColors(
    divider = Color(color = 0xFFEEF0F4),
    icon = Color(color = 0xFF4A4F5E),
    precipitation = Color(color = 0xFF2F6FE0)
)

internal val darkVadretColors: VadretColors = VadretColors(
    divider = Color(color = 0xFF262933),
    icon = Color(color = 0xFFC4C8D4),
    precipitation = Color(color = 0xFF7FB0FF),
    warningMessage = darkWarningMessage,
    onWarningMessage = onDarkWarningMessage
)

internal val amoledVadretColors: VadretColors = VadretColors(
    divider = Color(color = 0xFF1E1E1E),
    icon = Color(color = 0xFFC4C8D4),
    precipitation = Color(color = 0xFF7FB0FF),
    warningMessage = darkWarningMessage,
    onWarningMessage = onDarkWarningMessage
)
