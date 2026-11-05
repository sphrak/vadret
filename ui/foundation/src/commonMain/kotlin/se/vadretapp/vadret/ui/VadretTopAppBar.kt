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
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.app_name
import se.vadretapp.vadret.ui.resources.back
import se.vadretapp.vadret.ui.resources.ic_arrow_back

private val titleStartPadding = 5.dp

@Composable
fun VadretTopAppBar(
    modifier: Modifier = Modifier,
    title: String = stringResource(resource = Res.string.app_name),
    onBack: (() -> Unit)? = null,
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
                        painter = painterResource(resource = Res.drawable.ic_arrow_back),
                        contentDescription = stringResource(resource = Res.string.back)
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