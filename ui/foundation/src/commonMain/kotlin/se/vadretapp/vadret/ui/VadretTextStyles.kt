package se.vadretapp.vadret.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private const val TABULAR_NUMBERS = "tnum"

private val exactLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
    mode = LineHeightStyle.Mode.Tight
)

internal fun TextStyle.withTabularNumbers(): TextStyle = copy(fontFeatureSettings = TABULAR_NUMBERS)

private fun textStyle(
    fontSize: Int,
    lineHeight: Int,
    fontWeight: FontWeight = FontWeight.Normal
): TextStyle =
    TextStyle(
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        fontWeight = fontWeight,
        letterSpacing = 0.sp
    ).withTabularNumbers()

@Immutable
data class VadretTextStyles(
    val temperature: TextStyle = textStyle(
        fontSize = 55,
        lineHeight = 55,
        fontWeight = FontWeight.Light
    ).copy(
        letterSpacing = (-2).sp,
        lineHeightStyle = exactLineHeight
    ),
    val condition: TextStyle = textStyle(
        fontSize = 21,
        lineHeight = 25
    ).copy(lineHeightStyle = exactLineHeight),
    val reading: TextStyle = textStyle(
        fontSize = 21,
        lineHeight = 23
    ).copy(lineHeightStyle = exactLineHeight),
    val headline: TextStyle = textStyle(
        fontSize = 26,
        lineHeight = 31,
        fontWeight = FontWeight.Medium
    ).copy(letterSpacing = (-0.3).sp),
    val title: TextStyle = textStyle(
        fontSize = 21,
        lineHeight = 26,
        fontWeight = FontWeight.Medium
    ),
    val heading: TextStyle = textStyle(
        fontSize = 18,
        lineHeight = 24,
        fontWeight = FontWeight.Medium
    ),
    val value: TextStyle = textStyle(
        fontSize = 16,
        lineHeight = 22,
        fontWeight = FontWeight.Medium
    ),
    val listItem: TextStyle = textStyle(
        fontSize = 16,
        lineHeight = 22
    ),
    val body: TextStyle = textStyle(
        fontSize = 15,
        lineHeight = 22
    ),
    val detail: TextStyle = textStyle(
        fontSize = 14,
        lineHeight = 20
    ),
    val detailStrong: TextStyle = textStyle(
        fontSize = 14,
        lineHeight = 20,
        fontWeight = FontWeight.Medium
    ),
    val detailLabel: TextStyle = textStyle(
        fontSize = 13,
        lineHeight = 20
    ),
    val detailLabelStrong: TextStyle = textStyle(
        fontSize = 13,
        lineHeight = 20,
        fontWeight = FontWeight.Medium
    ),
    val label: TextStyle = textStyle(
        fontSize = 12,
        lineHeight = 16
    ),
    val labelStrong: TextStyle = textStyle(
        fontSize = 12,
        lineHeight = 16,
        fontWeight = FontWeight.Bold
    ),
    val attribution: TextStyle = textStyle(
        fontSize = 10,
        lineHeight = 14
    )
)