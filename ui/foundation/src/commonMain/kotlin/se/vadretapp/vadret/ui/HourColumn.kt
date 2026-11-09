package se.vadretapp.vadret.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.now

private const val SINGLE_LINE = 1
private val hourColumnWidth = 46.dp
private val nowHourCornerRadius = 12.dp
private val nowHourShape = RoundedCornerShape(size = nowHourCornerRadius)

val hourColumnBottomPadding: Dp = nowHourCornerRadius

@Composable
fun HourColumn(
    isNow: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val highlightColor: Color = if (isNow) {
        VadretTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(width = hourColumnWidth)
            .background(
                color = highlightColor,
                shape = nowHourShape
            )
            .padding(bottom = hourColumnBottomPadding)
            .semantics(mergeDescendants = true) {},
        content = content
    )
}

@Composable
fun HourLabel(
    timeText: String,
    isNow: Boolean,
    modifier: Modifier = Modifier
) {
    if (isNow) {
        Text(
            text = stringResource(resource = Res.string.now),
            style = VadretTheme.textStyles.labelStrong,
            color = VadretTheme.colorScheme.onSurface,
            maxLines = SINGLE_LINE,
            modifier = modifier
        )
    } else {
        Text(
            text = timeText,
            style = VadretTheme.textStyles.label,
            color = VadretTheme.colorScheme.onSurfaceVariant,
            maxLines = SINGLE_LINE,
            modifier = modifier
        )
    }
}