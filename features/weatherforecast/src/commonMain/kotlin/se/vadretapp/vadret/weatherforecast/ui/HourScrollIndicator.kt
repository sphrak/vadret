package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme
import kotlin.math.max

private const val HALF_DIVISOR = 2f
private const val THUMB_ALPHA = 0.5f
private const val TRACK_TOP = 0f
private const val PREVIEW_SHOWN_FRACTION = 0.3f
private const val PREVIEW_SCROLLED_FRACTION = 0.5f

private val trackWidth = 55.dp
private val trackHeight = 4.dp
private val minimumThumbWidth = 8.dp

@Composable
internal fun HourScrollIndicator(
    shownFraction: Float,
    scrolledFraction: () -> Float,
    modifier: Modifier = Modifier
) {
    val trackColor: Color = VadretTheme.colors.divider
    val thumbColor: Color = VadretTheme.colorScheme.onSurfaceVariant.copy(alpha = THUMB_ALPHA)
    Spacer(
        modifier = modifier
            .size(
                width = trackWidth,
                height = trackHeight
            )
            .drawBehind {
                val cornerRadius = CornerRadius(
                    x = size.height / HALF_DIVISOR,
                    y = size.height / HALF_DIVISOR
                )
                val thumbWidth: Float = max(
                    a = minimumThumbWidth.toPx(),
                    b = size.width * shownFraction
                )
                drawRoundRect(
                    color = trackColor,
                    cornerRadius = cornerRadius
                )
                drawRoundRect(
                    color = thumbColor,
                    topLeft = Offset(
                        x = (size.width - thumbWidth) * scrolledFraction(),
                        y = TRACK_TOP
                    ),
                    size = Size(
                        width = thumbWidth,
                        height = size.height
                    ),
                    cornerRadius = cornerRadius
                )
            }
    )
}

@Preview
@Composable
private fun HourScrollIndicatorPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        HourScrollIndicator(
            modifier = Modifier
                .background(color = VadretTheme.colorScheme.surface)
                .padding(all = VadretTheme.spacing.medium),
            shownFraction = PREVIEW_SHOWN_FRACTION,
            scrolledFraction = { PREVIEW_SCROLLED_FRACTION }
        )
    }
}