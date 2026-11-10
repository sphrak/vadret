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
    val temperatureZero: Color = Color(color = 0xFF5AA9D6),
    val temperatureMinusFive: Color = Color(color = 0xFF3F7FD0),
    val temperatureMinusTen: Color = Color(color = 0xFF3F51B5),
    val temperatureMinusFifteen: Color = Color(color = 0xFF2C3A8F),
    val temperatureMinusTwenty: Color = Color(color = 0xFF1F2D52),
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

private val darkTemperatureZero = Color(color = 0xFF8FC4EA)
private val darkTemperatureMinusFive = Color(color = 0xFF74A8F0)
private val darkTemperatureMinusTen = Color(color = 0xFF6F8FE6)
private val darkTemperatureMinusFifteen = Color(color = 0xFF6577DC)
private val darkTemperatureMinusTwenty = Color(color = 0xFF5B62CC)

internal val lightVadretColors: VadretColors = VadretColors(
    divider = Color(color = 0xFFEEF0F4),
    icon = Color(color = 0xFF4A4F5E),
    precipitation = Color(color = 0xFF2F6FE0)
)

internal val darkVadretColors: VadretColors = VadretColors(
    divider = Color(color = 0xFF262933),
    icon = Color(color = 0xFFC4C8D4),
    precipitation = Color(color = 0xFF7FB0FF),
    temperatureZero = darkTemperatureZero,
    temperatureMinusFive = darkTemperatureMinusFive,
    temperatureMinusTen = darkTemperatureMinusTen,
    temperatureMinusFifteen = darkTemperatureMinusFifteen,
    temperatureMinusTwenty = darkTemperatureMinusTwenty,
    warningMessage = darkWarningMessage,
    onWarningMessage = onDarkWarningMessage
)

internal val amoledVadretColors: VadretColors = VadretColors(
    divider = Color(color = 0xFF1E1E1E),
    icon = Color(color = 0xFFC4C8D4),
    precipitation = Color(color = 0xFF7FB0FF),
    temperatureZero = darkTemperatureZero,
    temperatureMinusFive = darkTemperatureMinusFive,
    temperatureMinusTen = darkTemperatureMinusTen,
    temperatureMinusFifteen = darkTemperatureMinusFifteen,
    temperatureMinusTwenty = darkTemperatureMinusTwenty,
    warningMessage = darkWarningMessage,
    onWarningMessage = onDarkWarningMessage
)