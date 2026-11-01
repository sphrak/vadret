package se.vadretapp.vadret.logging

import platform.Foundation.NSLog
import platform.Foundation.NSString

private const val LINE_FORMAT = "%@"

fun nsLogSink(minimumLevel: LogLevel): LogSink = DefaultNsLogSink(minimumLevel = minimumLevel)

internal class DefaultNsLogSink(
    private val minimumLevel: LogLevel
) : LogSink {

    override fun accepts(level: LogLevel): Boolean = level >= minimumLevel

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun write(
        level: LogLevel,
        tag: String,
        message: String
    ) {
        NSLog(LINE_FORMAT, "${level.name} $tag $message" as NSString)
    }
}