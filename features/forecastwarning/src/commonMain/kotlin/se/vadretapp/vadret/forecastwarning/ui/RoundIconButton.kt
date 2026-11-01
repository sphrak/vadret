package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.ui.VadretTheme

private const val EXPANDED = 0f
private const val OPAQUE = 1f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RoundIconButton(
    iconResource: DrawableResource,
    contentDescription: StringResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    collapsedContentColor: Color = VadretTheme.colorScheme.onSurface,
    collapsedFraction: () -> Float = { EXPANDED }
) {
    val containerColor: Color = VadretTheme.colorScheme.surface
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size = IconButtonDefaults.smallContainerSize())
            .clip(shape = CircleShape)
            .drawBehind {
                drawCircle(color = containerColor.copy(alpha = OPAQUE - collapsedFraction()))
            }.clickable(
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Icon(
            painter = painterResource(resource = iconResource),
            contentDescription = stringResource(resource = contentDescription),
            tint = VadretTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(size = IconButtonDefaults.smallIconSize)
                .graphicsLayer { alpha = OPAQUE - collapsedFraction() }
        )
        Icon(
            painter = painterResource(resource = iconResource),
            contentDescription = null,
            tint = collapsedContentColor,
            modifier = Modifier
                .size(size = IconButtonDefaults.smallIconSize)
                .graphicsLayer { alpha = collapsedFraction() }
        )
    }
}