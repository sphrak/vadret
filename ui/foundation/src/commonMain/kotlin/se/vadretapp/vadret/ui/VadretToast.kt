package se.vadretapp.vadret.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.almost_there
import kotlin.time.Duration.Companion.milliseconds

private const val NOTHING_SHOWN = 0

private val shownDuration = 2_000.milliseconds
private val toastShape = RoundedCornerShape(50f)

@Stable
class VadretToastState {

    internal var message: String by mutableStateOf(value = "")
        private set

    internal var shownCount: Int by mutableIntStateOf(value = NOTHING_SHOWN)
        private set

    fun show(message: String) {
        this.message = message
        shownCount++
    }
}

@Composable
fun rememberVadretToastState(): VadretToastState = remember { VadretToastState() }

@Composable
fun VadretToast(
    state: VadretToastState,
    modifier: Modifier = Modifier
) {
    var isVisible: Boolean by remember { mutableStateOf(value = false) }
    LaunchedEffect(state.shownCount) {
        if (state.shownCount != NOTHING_SHOWN) {
            isVisible = true
            delay(duration = shownDuration)
            isVisible = false
        }
    }
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        ToastMessage(message = state.message)
    }
}

@Composable
private fun ToastMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        style = VadretTheme.textStyles.detail,
        color = Color.White,
        modifier = modifier
            .background(
                color = Color(0xE6333333),
                shape = toastShape
            )
            .padding(
                horizontal = 24.dp,
                vertical = 12.dp
            )
    )
}

@Preview
@Composable
private fun ToastMessagePreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        Box(
            modifier = Modifier
                .background(color = VadretTheme.colorScheme.background)
                .padding(all = VadretTheme.spacing.extraLarge)
        ) {
            ToastMessage(message = stringResource(resource = Res.string.almost_there))
        }
    }
}