package se.vadretapp.vadret.settings.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal actual fun rememberAppIcon(): ImageBitmap? {
    val context: Context = LocalContext.current
    val appIcon: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, context) {
        value = withContext(context = Dispatchers.Default) {
            context.packageManager
                .getApplicationIcon(context.packageName)
                .toBitmap()
                .asImageBitmap()
        }
    }
    return appIcon
}