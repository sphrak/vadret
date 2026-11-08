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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.ui.model.NavigationIcon
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.app_name
import se.vadretapp.vadret.ui.resources.back
import se.vadretapp.vadret.ui.resources.close
import se.vadretapp.vadret.ui.resources.ic_arrow_back
import se.vadretapp.vadret.ui.resources.ic_close

private val titleStartPadding = 5.dp

@Composable
fun VadretTopAppBar(
    modifier: Modifier = Modifier,
    title: String = stringResource(resource = Res.string.app_name),
    onBack: (() -> Unit)? = null,
    navigationIcon: NavigationIcon = NavigationIcon.BACK,
    actions: @Composable RowScope.() -> Unit = {}
) {
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

private fun NavigationIcon.iconResource(): DrawableResource =
    when (this) {
        NavigationIcon.BACK -> Res.drawable.ic_arrow_back
        NavigationIcon.CLOSE -> Res.drawable.ic_close
    }

private fun NavigationIcon.descriptionResource(): StringResource =
    when (this) {
        NavigationIcon.BACK -> Res.string.back
        NavigationIcon.CLOSE -> Res.string.close
    }