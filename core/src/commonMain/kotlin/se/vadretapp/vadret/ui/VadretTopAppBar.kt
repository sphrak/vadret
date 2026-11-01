package se.vadretapp.vadret.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.app_name

private val titleStartPadding = 5.dp

@Composable
fun VadretTopAppBar(
    modifier: Modifier = Modifier,
    title: String = stringResource(resource = Res.string.app_name),
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = VadretTheme.textStyles.title,
                modifier = Modifier.padding(start = titleStartPadding)
            )
        },
        actions = actions,
        expandedHeight = TopAppBarDefaults.TopAppBarExpandedHeight,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = VadretTheme.colorScheme.background,
            titleContentColor = VadretTheme.colorScheme.onBackground,
            actionIconContentColor = VadretTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    )
}