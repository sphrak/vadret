package se.vadretapp.vadret.logging

import android.util.Log

fun logcatSink(minimumLevel: LogLevel): LogSink = DefaultLogcatSink(minimumLevel = minimumLevel)

internal class DefaultLogcatSink(
    private val minimumLevel: LogLevel
) : LogSink {

    override fun accepts(level: LogLevel): Boolean = level >= minimumLevel

    override fun write(
        level: LogLevel,
        tag: String,
        message: String
    ) {
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
    }
}