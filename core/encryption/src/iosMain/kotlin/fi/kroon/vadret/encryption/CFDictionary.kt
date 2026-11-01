@file:OptIn(ExperimentalForeignApi::class)

package fi.kroon.vadret.encryption

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.ptr
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks

internal fun cfDictionaryOf(entries: List<Pair<CFStringRef?, CFTypeRef?>>): CFMutableDictionaryRef? {
    val dictionary = CFDictionaryCreateMutable(
        kCFAllocatorDefault,
        entries.size.convert(),
        kCFTypeDictionaryKeyCallBacks.ptr,
        kCFTypeDictionaryValueCallBacks.ptr
    )
    for ((key, value) in entries) {
        CFDictionaryAddValue(dictionary, key, value)
    }
    return dictionary
}