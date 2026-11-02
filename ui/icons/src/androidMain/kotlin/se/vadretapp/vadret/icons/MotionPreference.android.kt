package se.vadretapp.vadret.icons

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val DEFAULT_ANIMATOR_SCALE = 1f
private const val NO_ANIMATOR_SCALE = 0f

@Composable
actual fun rememberIsMotionEnabled(): Boolean {
    val context: Context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, DEFAULT_ANIMATOR_SCALE) > NO_ANIMATOR_SCALE
    }
}