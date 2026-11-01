@file:OptIn(ExperimentalForeignApi::class)

package se.vadretapp.vadret.encryption.extension

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFStringEncodingUTF8

internal fun String.toCFString(): CFStringRef? = CFStringCreateWithCString(kCFAllocatorDefault, this, kCFStringEncodingUTF8)