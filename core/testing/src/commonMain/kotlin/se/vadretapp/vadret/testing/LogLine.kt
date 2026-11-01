package se.vadretapp.vadret.testing

import se.vadretapp.vadret.logging.LogLevel

data class LogLine(
    val level: LogLevel,
    val tag: String,
    val message: String
)