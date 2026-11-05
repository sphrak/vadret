package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.settings.AboutEvent
import se.vadretapp.vadret.settings.AboutState
import se.vadretapp.vadret.settings.AboutViewModel
import se.vadretapp.vadret.settings.model.AboutRow
import se.vadretapp.vadret.settings.model.AboutRowAction
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.SettingsRow
import se.vadretapp.vadret.ui.SettingsScaffold
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.VadretToast
import se.vadretapp.vadret.ui.VadretToastState
import se.vadretapp.vadret.ui.rememberVadretToastState
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.about_app_description
import se.vadretapp.vadret.ui.resources.app_name
import se.vadretapp.vadret.ui.resources.settings_about

private const val HEADER_KEY = "header"

// The app icon is a large leading image, which Material3 lists show at this size.
private val appIconSize: Dp = 56.dp

private enum class AboutContentType {
    HEADER,
    ROW
}

@Composable
fun AboutRoute(
    onLicensesClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AboutViewModel = koinViewModel()
) {
    val state: AboutState by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.onEvent(event = AboutEvent.OnStart)
    }
    AboutScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onLicensesClick = onLicensesClick,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun AboutScreen(
    state: AboutState,
    onEvent: (AboutEvent) -> Unit,
    onLicensesClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val toastState: VadretToastState = rememberVadretToastState()
    val toastResource: StringResource? = state.toastResource
    if (toastResource != null) {
        val toastMessage: String = stringResource(resource = toastResource)
        LaunchedEffect(toastResource) {
            toastState.show(message = toastMessage)
            onEvent(AboutEvent.OnToastShown)
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        SettingsScaffold(
            title = stringResource(resource = Res.string.settings_about),
            onBack = onBack
        ) {
            item(key = HEADER_KEY, contentType = AboutContentType.HEADER) {
                AboutHeader()
            }
            items(
                items = state.aboutRows,
                key = { aboutRow: AboutRow -> aboutRow.titleResource.key },
                contentType = { AboutContentType.ROW }
            ) { aboutRow: AboutRow ->
                AboutRowItem(
                    aboutRow = aboutRow,
                    onClick = aboutRow.action.onClick(
                        onEvent = onEvent,
                        onLicensesClick = onLicensesClick
                    )
                )
            }
        }
        VadretToast(
            state = toastState,
            modifier = Modifier.align(alignment = Alignment.BottomCenter)
        )
    }
    val changelog: Changelog? = state.changelog
    if (state.isChangelogVisible && changelog != null) {
        ChangelogSheet(
            changelog = changelog,
            onDismiss = { onEvent(AboutEvent.OnChangelogDismissed) }
        )
    }
}

@Composable
private fun AboutHeader(modifier: Modifier = Modifier) {
    val loadedAppIcon: ImageBitmap? = rememberAppIcon()
    ListItem(
        modifier = modifier,
        leadingContent = {
            if (loadedAppIcon != null) {
                Image(
                    modifier = Modifier
                        .size(size = appIconSize)
                        .clip(shape = CircleShape),
                    bitmap = loadedAppIcon,
                    contentDescription = null
                )
            } else {
                Spacer(modifier = Modifier.size(size = appIconSize))
            }
        },
        supportingContent = { Text(text = stringResource(resource = Res.string.about_app_description)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    ) {
        Text(text = stringResource(resource = Res.string.app_name))
    }
}

@Composable
private fun AboutRowAction.onClick(
    onEvent: (AboutEvent) -> Unit,
    onLicensesClick: () -> Unit
): () -> Unit {
    val uriHandler: UriHandler = LocalUriHandler.current
    return when (this) {
        is AboutRowAction.OpenUrl -> {
            val url: String = stringResource(resource = urlResource)
            val openUrl: () -> Unit = { uriHandler.openUri(uri = url) }
            openUrl
        }
        AboutRowAction.ShowChangelog -> {
            val showChangelog: () -> Unit = { onEvent(AboutEvent.OnChangelogClicked) }
            showChangelog
        }
        AboutRowAction.ShowLicenses -> onLicensesClick
        AboutRowAction.CountVersionTap -> {
            val countVersionTap: () -> Unit = { onEvent(AboutEvent.OnVersionClicked) }
            countVersionTap
        }
    }
}

@Composable
private fun AboutRowItem(
    aboutRow: AboutRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trailingIconResource: DrawableResource? = aboutRow.trailingIconResource
    SettingsRow(
        title = stringResource(resource = aboutRow.titleResource),
        summary = aboutRow.summary?.text(),
        iconResource = aboutRow.iconResource,
        onClick = onClick,
        modifier = modifier,
        trailingContent = if (trailingIconResource != null) {
            {
                Icon(
                    painter = painterResource(resource = trailingIconResource),
                    contentDescription = null
                )
            }
        } else {
            null
        }
    )
}

@Preview(device = Devices.PHONE)
@Composable
private fun AboutScreenPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        AboutScreen(
            state = aboutPreviewState(),
            onEvent = {},
            onLicensesClick = {},
            onBack = {},
            modifier = Modifier.background(color = VadretTheme.colorScheme.background)
        )
    }
}