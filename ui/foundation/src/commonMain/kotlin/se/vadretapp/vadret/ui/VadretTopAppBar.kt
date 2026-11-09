package se.vadretapp.vadret.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.ui.model.NavigationIcon
import se.vadretapp.vadret.ui.model.descriptionResource
import se.vadretapp.vadret.ui.model.iconResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.app_name

private val titleStartPadding = 5.dp

@Composable
fun VadretTopAppBar(
    modifier: Modifier = Modifier,
    title: String = stringResource(resource = Res.string.app_name),
    onBack: (() -> Unit)? = null,
    navigationIcon: NavigationIcon = NavigationIcon.BACK,
    actions: @Composable RowScope.() -> Unit = {}
) {
    if (LocalNativeNavigationBar.current) {
        return
    }
    TopAppBar(
        title = {
            Text(
                modifier = if (onBack == null) {
                    Modifier.padding(start = titleStartPadding)
                } else {
                    Modifier
                },
                text = title,
                style = VadretTheme.textStyles.title
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(resource = navigationIcon.iconResource()),
                        contentDescription = stringResource(resource = navigationIcon.descriptionResource())
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = VadretTheme.colorScheme.background,
            titleContentColor = VadretTheme.colorScheme.onBackground,
            navigationIconContentColor = VadretTheme.colorScheme.onBackground,
            actionIconContentColor = VadretTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    )
}