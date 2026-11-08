package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.forecastwarning.WarningEvent
import se.vadretapp.vadret.forecastwarning.WarningState
import se.vadretapp.vadret.forecastwarning.WarningViewModel
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.LocalHourFormat
import se.vadretapp.vadret.ui.LocalLanguageTag
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretCard
import se.vadretapp.vadret.ui.VadretProgressBar
import se.vadretapp.vadret.ui.VadretPullToRefreshBox
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.VadretTopAppBar
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ic_filter_list
import se.vadretapp.vadret.ui.resources.settings_places
import se.vadretapp.vadret.ui.resources.warning_display_no_warnings_issued
import se.vadretapp.vadret.ui.resources.warnings

private const val WARNING_LIST_KEY = "warning-list"

private val contentHorizontalPadding = 13.dp
private val contentTopPadding = 8.dp
private val contentBottomPadding = 21.dp
private val listCornerRadius = 21.dp
private val listVerticalPadding = 13.dp

@Composable
fun WarningRoute(
    onWarningClick: (String) -> Unit,
    onFollowedPlacesClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WarningViewModel = koinViewModel()
) {
    val state: WarningState by viewModel.state.collectAsStateWithLifecycle()
    WarningScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onWarningClick = onWarningClick,
        onFollowedPlacesClick = onFollowedPlacesClick,
        modifier = modifier
    )
}

@Composable
internal fun WarningScreen(
    state: WarningState,
    onEvent: (WarningEvent) -> Unit,
    onWarningClick: (String) -> Unit,
    onFollowedPlacesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val languageTag: String = LocalLanguageTag.current
    val hourFormat: HourFormat = LocalHourFormat.current
    LaunchedEffect(languageTag, hourFormat) {
        onEvent(
            WarningEvent.OnLocaleChanged(
                languageTag = languageTag,
                hourFormat = hourFormat
            )
        )
    }
    LaunchedEffect(Unit) {
        onEvent(WarningEvent.OnStart)
    }
    val errorMessageResource: StringResource? = state.errorMessageResource
    if (errorMessageResource != null) {
        val message: String = stringResource(resource = errorMessageResource)
        LaunchedEffect(errorMessageResource) {
            snackbarHostState.showSnackbar(message = message)
            onEvent(WarningEvent.OnErrorShown)
        }
    }

    Scaffold(
        topBar = {
            VadretTopAppBar(
                title = stringResource(resource = Res.string.warnings),
                actions = {
                    IconButton(onClick = onFollowedPlacesClick) {
                        Icon(
                            painter = painterResource(resource = Res.drawable.ic_filter_list),
                            contentDescription = stringResource(resource = Res.string.settings_places)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = VadretTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(),
        modifier = modifier
    ) { innerPadding: PaddingValues ->
        VadretPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(WarningEvent.OnRefresh) },
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = contentHorizontalPadding,
                    top = contentTopPadding,
                    end = contentHorizontalPadding,
                    bottom = contentBottomPadding
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                warningList(
                    warnings = state.warnings,
                    onWarningClick = onWarningClick
                )
            }
            if (state.isLoading) {
                VadretProgressBar(modifier = Modifier.align(alignment = Alignment.TopCenter))
            }
            if (state.isEmpty) {
                Text(
                    text = stringResource(resource = Res.string.warning_display_no_warnings_issued),
                    style = VadretTheme.textStyles.body,
                    color = VadretTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(alignment = Alignment.Center)
                        .padding(all = VadretTheme.spacing.extraLarge)
                )
            }
        }
    }
}

private fun LazyListScope.warningList(
    warnings: ImmutableList<WarningItem>,
    onWarningClick: (String) -> Unit
) {
    if (warnings.isEmpty()) {
        return
    }
    item(
        key = WARNING_LIST_KEY,
        contentType = WARNING_LIST_KEY
    ) {
        VadretCard(shape = RoundedCornerShape(size = listCornerRadius)) {
            warnings.forEachIndexed { index: Int, warning: WarningItem ->
                val isFirst: Boolean = index == 0
                val isLast: Boolean = index == warnings.lastIndex
                if (!isFirst) {
                    HorizontalDivider(
                        color = VadretTheme.colors.divider,
                        modifier = Modifier.padding(
                            start = rowDividerStartPadding,
                            end = rowDividerEndPadding
                        )
                    )
                }
                WarningRow(
                    warning = warning,
                    onClick = { onWarningClick(warning.identifier) },
                    shape = RoundedCornerShape(
                        topStart = listCornerRadius.onlyOnEdge(isEdge = isFirst),
                        topEnd = listCornerRadius.onlyOnEdge(isEdge = isFirst),
                        bottomStart = listCornerRadius.onlyOnEdge(isEdge = isLast),
                        bottomEnd = listCornerRadius.onlyOnEdge(isEdge = isLast)
                    ),
                    edgeTopPadding = listVerticalPadding.onlyOnEdge(isEdge = isFirst),
                    edgeBottomPadding = listVerticalPadding.onlyOnEdge(isEdge = isLast)
                )
            }
        }
    }
}

private fun Dp.onlyOnEdge(isEdge: Boolean): Dp =
    if (isEdge) {
        this
    } else {
        0.dp
    }

@Preview(device = Devices.PHONE)
@Composable
private fun WarningScreenPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        WarningScreen(
            state = warningPreviewState(),
            onEvent = {},
            onWarningClick = {},
            onFollowedPlacesClick = {}
        )
    }
}