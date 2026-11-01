package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.ui.VadretSlidingSelector
import se.vadretapp.vadret.ui.VadretTheme

private val tabsHeight = 44.dp
private val tabsInnerPadding = 4.dp
private val tabsBorderWidth = 1.dp

@Composable
internal fun SettingsTabs(
    pages: ImmutableList<SettingsPage>,
    selectedPage: SettingsPage,
    onPageClick: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = VadretTheme.colorScheme.surface,
        border = BorderStroke(
            width = tabsBorderWidth,
            color = VadretTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(height = tabsHeight)
    ) {
        VadretSlidingSelector(
            optionCount = pages.size,
            selectedIndex = pages.indexOf(element = selectedPage),
            onOptionSelected = { index: Int -> onPageClick(pages[index]) },
            role = Role.Tab,
            isEqualWidth = true,
            modifier = Modifier
                .fillMaxSize()
                .padding(all = tabsInnerPadding)
        ) { index: Int, contentColor: Color, _: Float ->
            Text(
                text = stringResource(resource = pages[index].titleResource),
                style = VadretTheme.textStyles.bodyStrong,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}