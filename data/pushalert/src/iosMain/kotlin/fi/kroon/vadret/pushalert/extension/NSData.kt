@file:OptIn(ExperimentalForeignApi::class)

package fi.kroon.vadret.pushalert.extension

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import platform.Foundation.NSData

internal fun NSData.toByteArray(): ByteArray {
    val dataBytes = bytes ?: return ByteArray(size = 0)
    return dataBytes.reinterpret<ByteVar>().readBytes(count = length.toInt())
}