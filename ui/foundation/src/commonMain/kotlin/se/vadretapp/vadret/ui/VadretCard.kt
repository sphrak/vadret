package se.vadretapp.vadret.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.app_name

private val cardBorderWidth = 1.dp

@Composable
fun VadretCard(
    modifier: Modifier = Modifier,
    shape: Shape = VadretTheme.shapes.large,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = VadretTheme.colorScheme.surface,
        border = BorderStroke(
            width = cardBorderWidth,
            color = VadretTheme.colorScheme.outlineVariant
        )
    ) {
        Column(content = content)
    }
}

@Preview
@Composable
private fun VadretCardPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        VadretCard(
            modifier = Modifier
                .background(color = VadretTheme.colorScheme.background)
                .padding(all = VadretTheme.spacing.medium)
        ) {
            Text(
                text = stringResource(resource = Res.string.app_name),
                style = VadretTheme.textStyles.heading,
                color = VadretTheme.colorScheme.onSurface,
                modifier = Modifier.padding(all = VadretTheme.spacing.large)
            )
        }
    }
}