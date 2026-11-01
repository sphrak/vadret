package se.vadretapp.vadret.logging

interface LogSink {
    fun accepts(level: LogLevel): Boolean
    fun write(
        level: LogLevel,
        tag: String,
        message: String
    )
}