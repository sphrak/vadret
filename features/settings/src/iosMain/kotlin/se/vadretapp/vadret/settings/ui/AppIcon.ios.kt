package se.vadretapp.vadret.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

private const val BUNDLE_ICONS_KEY = "CFBundleIcons"
private const val PRIMARY_ICON_KEY = "CFBundlePrimaryIcon"
private const val ICON_FILES_KEY = "CFBundleIconFiles"
private const val FIRST_BYTE_INDEX = 0
private const val NO_BYTES = 0

@Composable
internal actual fun rememberAppIcon(): ImageBitmap? {
    val appIcon: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null) {
        value = withContext(context = Dispatchers.Default) {
            primaryIcon()?.toPngBytes()?.decodeToImageBitmap()
        }
    }
    return appIcon
}

private fun primaryIcon(): UIImage? {
    val bundleIcons: Map<*, *>? = NSBundle.mainBundle.objectForInfoDictionaryKey(key = BUNDLE_ICONS_KEY) as? Map<*, *>
    val primaryIcon: Map<*, *>? = bundleIcons?.get(PRIMARY_ICON_KEY) as? Map<*, *>
    val iconName: String? = (primaryIcon?.get(ICON_FILES_KEY) as? List<*>)?.lastOrNull() as? String
    return if (iconName != null) {
        UIImage.imageNamed(name = iconName)
    } else {
        null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.toPngBytes(): ByteArray? {
    val pngData: NSData = UIImagePNGRepresentation(image = this) ?: return null
    val size: Int = pngData.length.toInt()
    if (size == NO_BYTES) {
        return null
    }
    val bytes = ByteArray(size = size)
    bytes.usePinned { pinnedBytes ->
        memcpy(pinnedBytes.addressOf(index = FIRST_BYTE_INDEX), pngData.bytes, pngData.length)
    }
    return bytes
}