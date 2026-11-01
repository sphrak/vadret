package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.mikepenz.markdown.compose.LazyMarkdownSuccess
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.markdownPadding
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme

@OptIn(ExperimentalMaterial3Api::class)
private val expandedOnlyValues: Set<SheetValue> = setOf(SheetValue.Hidden, SheetValue.Expanded)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChangelogSheet(
    changelog: Changelog,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = expandedOnlyValues
        ),
        containerColor = VadretTheme.colorScheme.surface,
        modifier = modifier
    ) {
        ChangelogContent(changelog = changelog)
    }
}

@Composable
private fun ChangelogContent(
    changelog: Changelog,
    modifier: Modifier = Modifier
) {
    val contentPadding = PaddingValues(
        start = VadretTheme.spacing.extraLarge,
        end = VadretTheme.spacing.extraLarge,
        bottom = VadretTheme.spacing.extraLarge
    )
    Markdown(
        content = changelog.markdown,
        colors = markdownColor(text = VadretTheme.colorScheme.onSurface),
        typography = changelogTypography(),
        padding = markdownPadding(
            block = VadretTheme.spacing.medium,
            list = VadretTheme.spacing.small,
            listItemTop = VadretTheme.spacing.extraSmall,
            listItemBottom = VadretTheme.spacing.extraSmall
        ),
        success = { state: State.Success, components: MarkdownComponents, successModifier: Modifier ->
            LazyMarkdownSuccess(
                state = state,
                components = components,
                contentPadding = contentPadding,
                modifier = successModifier
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    )
}

@Composable
private fun changelogTypography(): MarkdownTypography {
    val body: TextStyle = VadretTheme.typography.bodyLarge
    val subheading: TextStyle = VadretTheme.typography.titleMedium
    return markdownTypography(
        h1 = VadretTheme.typography.headlineSmall,
        h2 = VadretTheme.typography.titleLarge,
        h3 = subheading,
        h4 = subheading,
        h5 = subheading,
        h6 = subheading,
        text = body,
        paragraph = body,
        ordered = body,
        bullet = body,
        list = body,
        textLink = TextLinkStyles(
            style = SpanStyle(
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline
            )
        )
    )
}

@Preview
@Composable
private fun ChangelogContentPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        ChangelogContent(
            changelog = changelogPreview(),
            modifier = Modifier.background(color = VadretTheme.colorScheme.surface)
        )
    }
}