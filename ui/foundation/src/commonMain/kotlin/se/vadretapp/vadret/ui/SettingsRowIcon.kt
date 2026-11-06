package se.vadretapp.vadret.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.vadretapp.vadret.icons.VadretIcon
import se.vadretapp.vadret.icons.VadretIconGraphic

private val settingsRowIconSize: Dp = 24.dp

// An icon from VadretIcons, coloured like the drawable icons in the other rows.
@Composable
fun SettingsRowIcon(
    icon: VadretIconGraphic,
    modifier: Modifier = Modifier
) {
    VadretIcon(
        icon = icon,
        contentDescription = null,
        tint = LocalContentColor.current,
        modifier = modifier.size(size = settingsRowIconSize)
    )
}

// Takes the room of an icon, so the text of a row without one lines up with the rows above and below that have one.
@Composable
fun SettingsRowIconSpace(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.size(size = settingsRowIconSize))
}