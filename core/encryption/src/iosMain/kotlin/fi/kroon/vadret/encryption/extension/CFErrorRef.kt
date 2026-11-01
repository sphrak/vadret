@file:OptIn(ExperimentalForeignApi::class)

package fi.kroon.vadret.encryption.extension

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFErrorCopyDescription
import platform.CoreFoundation.CFErrorRef
import platform.CoreFoundation.CFRelease
import platform.Foundation.CFBridgingRelease

internal fun CFErrorRef?.describeAndRelease(): String {
    if (this == null) {
        return NO_DESCRIPTION
    }
    val description = CFBridgingRelease(CFErrorCopyDescription(this)) as? String ?: NO_DESCRIPTION
    CFRelease(this)
    return description
}

private const val NO_DESCRIPTION = "no description"