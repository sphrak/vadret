@file:OptIn(ExperimentalForeignApi::class)

package fi.kroon.vadret.encryption.extension

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.kCFAllocatorDefault

internal fun ByteArray.toCFData(): CFDataRef? {
    if (isEmpty()) {
        return CFDataCreate(kCFAllocatorDefault, null, 0)
    }
    return usePinned { pinned -> CFDataCreate(kCFAllocatorDefault, pinned.addressOf(index = 0).reinterpret<UByteVar>(), size.convert()) }
}

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