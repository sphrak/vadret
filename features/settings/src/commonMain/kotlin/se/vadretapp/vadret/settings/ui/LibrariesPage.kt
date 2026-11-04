package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.settings.model.LibraryGroup
import se.vadretapp.vadret.ui.SettingsRowDivider
import se.vadretapp.vadret.ui.SettingsSectionHeader
import se.vadretapp.vadret.ui.SettingsValueRow
import se.vadretapp.vadret.ui.VadretCard
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.licenses_intro

private const val INTRO_KEY = "intro"
private const val INTRO_CONTENT_TYPE = "intro"
private const val LIBRARY_GROUP_CONTENT_TYPE = "library-group"

@Composable
internal fun LibrariesPage(
    libraryGroups: ImmutableList<LibraryGroup>,
    onLibraryClick: (Library) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = VadretTheme.spacing.large,
            end = VadretTheme.spacing.large,
            bottom = VadretTheme.spacing.large
        ),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = INTRO_KEY, contentType = INTRO_CONTENT_TYPE) {
            Text(
                text = stringResource(resource = Res.string.licenses_intro),
                style = VadretTheme.textStyles.body,
                color = VadretTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = VadretTheme.spacing.small,
                    end = VadretTheme.spacing.small,
                    top = VadretTheme.spacing.large
                )
            )
        }
        items(
            items = libraryGroups,
            key = { libraryGroup: LibraryGroup -> libraryGroup.author },
            contentType = { LIBRARY_GROUP_CONTENT_TYPE }
        ) { libraryGroup: LibraryGroup ->
            LibraryGroupSection(
                libraryGroup = libraryGroup,
                onLibraryClick = onLibraryClick
            )
        }
    }
}

@Composable
private fun LibraryGroupSection(
    libraryGroup: LibraryGroup,
    onLibraryClick: (Library) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SettingsSectionHeader(text = libraryGroup.author)
        VadretCard {
            libraryGroup.libraries.forEachIndexed { index: Int, library: Library ->
                if (index > 0) {
                    SettingsRowDivider()
                }
                SettingsValueRow(
                    title = library.title,
                    value = library.license,
                    onClick = { onLibraryClick(library) }
                )
            }
        }
    }
}