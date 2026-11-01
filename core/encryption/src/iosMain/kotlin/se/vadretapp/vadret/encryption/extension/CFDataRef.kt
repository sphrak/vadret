@file:OptIn(ExperimentalForeignApi::class)

package se.vadretapp.vadret.encryption.extension

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef

internal fun CFDataRef.toByteArray(): ByteArray {
    val length = CFDataGetLength(this).toInt()
    val bytes = CFDataGetBytePtr(this) ?: return ByteArray(size = 0)
    return bytes.reinterpret<ByteVar>().readBytes(count = length)
}