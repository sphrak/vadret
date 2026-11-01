@file:OptIn(ExperimentalForeignApi::class)

package fi.kroon.vadret.pushalert.extension

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

internal fun ByteArray.sha256(): ByteArray {
    val digest = UByteArray(size = CC_SHA256_DIGEST_LENGTH)
    digest.usePinned { output ->
        if (isEmpty()) {
            CC_SHA256(null, 0.convert(), output.addressOf(index = 0))
        } else {
            usePinned { input -> CC_SHA256(input.addressOf(index = 0), size.convert(), output.addressOf(index = 0)) }
        }
    }
    return digest.toByteArray()
}

internal fun ByteArray.toNSData(): NSData {
    if (isEmpty()) {
        return NSData()
    }
    return usePinned { pinned -> NSData.dataWithBytes(bytes = pinned.addressOf(index = 0), length = size.convert()) }
}