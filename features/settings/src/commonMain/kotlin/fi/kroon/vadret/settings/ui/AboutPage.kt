package fi.kroon.vadret.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import fi.kroon.vadret.core.resources.Res
import fi.kroon.vadret.core.resources.about_app_description
import fi.kroon.vadret.core.resources.app_name
import fi.kroon.vadret.settings.model.AboutRow
import fi.kroon.vadret.settings.model.AboutRowAction
import fi.kroon.vadret.settings.model.AboutRowHint
import fi.kroon.vadret.ui.SettingsRowDivider
import fi.kroon.vadret.ui.VadretCard
import fi.kroon.vadret.ui.VadretTheme
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val HEADER_KEY = "header"
private const val ROWS_KEY = "rows"
private const val HEADER_CONTENT_TYPE = "header"
private const val ROWS_CONTENT_TYPE = "rows"
private val appIconSize = 56.dp
private val rowIconSize = 24.dp
private val trailingIconSize = 20.dp
private val rowMinimumHeight = 56.dp

@Composable
internal fun AboutPage(
    aboutRows: ImmutableList<AboutRow>,
    onChangelogClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(all = VadretTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.medium),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = HEADER_KEY, contentType = HEADER_CONTENT_TYPE) {
            AboutHeader()
        }
        item(key = ROWS_KEY, contentType = ROWS_CONTENT_TYPE) {
            VadretCard {
                aboutRows.forEachIndexed { index: Int, aboutRow: AboutRow ->
                    if (index > 0) {
                        SettingsRowDivider()
                    }
                    AboutRowItem(
                        aboutRow = aboutRow,
                        onClick = aboutRow.action.onClick(onChangelogClick = onChangelogClick)
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutHeader(modifier: Modifier = Modifier) {
    val loadedAppIcon: ImageBitmap? = rememberAppIcon()
    VadretCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(all = VadretTheme.spacing.large)
        ) {
            if (loadedAppIcon != null) {
                Image(
                    bitmap = loadedAppIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(size = appIconSize)
                        .clip(shape = CircleShape)
                )
            } else {
                Spacer(modifier = Modifier.size(size = appIconSize))
            }
            Column {
                Text(
                    text = stringResource(resource = Res.string.app_name),
                    style = VadretTheme.textStyles.title,
                    color = VadretTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(resource = Res.string.about_app_description),
                    style = VadretTheme.textStyles.body,
                    color = VadretTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AboutRowAction.onClick(onChangelogClick: () -> Unit): () -> Unit {
    val uriHandler: UriHandler = LocalUriHandler.current
    return when (this) {
        is AboutRowAction.OpenUrl -> {
            val url: String = stringResource(resource = urlResource)
            val openUrl: () -> Unit = { uriHandler.openUri(uri = url) }
            openUrl
        }
        AboutRowAction.ShowChangelog -> onChangelogClick
    }
}

@Composable
private fun AboutRowItem(
    aboutRow: AboutRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hint: AboutRowHint? = aboutRow.hint
    val trailingIconResource: DrawableResource? = aboutRow.trailingIconResource
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = rowMinimumHeight)
            .clickable(onClick = onClick)
            .padding(
                horizontal = VadretTheme.spacing.large,
                vertical = VadretTheme.spacing.medium
            )
    ) {
        Icon(
            painter = painterResource(resource = aboutRow.iconResource),
            contentDescription = null,
            tint = VadretTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size = rowIconSize)
        )
        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = stringResource(resource = aboutRow.titleResource),
                style = VadretTheme.textStyles.listItem,
                color = VadretTheme.colorScheme.onSurface
            )
            if (hint != null) {
                Text(
                    text = hint.text(),
                    style = VadretTheme.textStyles.detail,
                    color = VadretTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailingIconResource != null) {
            Icon(
                painter = painterResource(resource = trailingIconResource),
                contentDescription = null,
                tint = VadretTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size = trailingIconSize)
            )
        }
    }
}

@Composable
private fun AboutRowHint.text(): String =
    when (this) {
        is AboutRowHint.Resource -> stringResource(resource = resource)
        is AboutRowHint.Plain -> text
    }