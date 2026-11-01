package se.vadretapp.vadret.testing

import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.logging.LogSink
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.update

class RecordingLogSink : LogSink {

    private val recordedLines: AtomicReference<List<LogLine>> = AtomicReference(value = emptyList())

    val lines: List<LogLine>
        get() = recordedLines.load()

    override fun accepts(level: LogLevel): Boolean = true

    override fun write(
        level: LogLevel,
        tag: String,
        message: String
    ) {
        recordedLines.update { recorded: List<LogLine> ->
            recorded + LogLine(
                level = level,
                tag = tag,
                message = message
            )
        }
    }
}