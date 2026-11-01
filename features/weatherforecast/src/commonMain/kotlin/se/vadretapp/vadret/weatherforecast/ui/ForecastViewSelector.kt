package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.ui.VadretSlidingSelector
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.weatherforecast.model.ForecastView

private val selectorHeight = 42.dp
private val selectorInnerPadding = 4.dp
private val selectorBorderWidth = 1.dp
private val optionIconSize = 18.dp
private val optionIconSpacing = 6.dp

@Composable
internal fun ForecastViewSelector(
    forecastView: ForecastView,
    onForecastViewSelected: (ForecastView) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = VadretTheme.colorScheme.surface,
        border = BorderStroke(
            width = selectorBorderWidth,
            color = VadretTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(height = selectorHeight)
    ) {
        VadretSlidingSelector(
            optionCount = ForecastView.entries.size,
            selectedIndex = forecastView.ordinal,
            onOptionSelected = { index: Int -> onForecastViewSelected(ForecastView.entries[index]) },
            role = Role.Tab,
            isEqualWidth = true,
            modifier = Modifier
                .fillMaxSize()
                .padding(all = selectorInnerPadding)
        ) { index: Int, contentColor: Color, _: Float ->
            val option: ForecastView = ForecastView.entries[index]
            Icon(
                painter = painterResource(resource = option.iconResource),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(size = optionIconSize)
            )
            Spacer(modifier = Modifier.width(width = optionIconSpacing))
            Text(
                text = stringResource(resource = option.titleResource),
                style = VadretTheme.textStyles.detailStrong,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}