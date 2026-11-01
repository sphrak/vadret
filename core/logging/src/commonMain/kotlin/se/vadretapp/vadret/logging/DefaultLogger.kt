package se.vadretapp.vadret.logging

internal class DefaultLogger(
    private val tag: String,
    private val sinks: () -> Collection<LogSink>
) : Logger {

    override fun debug(message: () -> String) {
        log(
            level = LogLevel.DEBUG,
            message = message
        )
    }

    override fun info(message: () -> String) {
        log(
            level = LogLevel.INFO,
            message = message
        )
    }

    override fun warn(message: () -> String) {
        log(
            level = LogLevel.WARN,
            message = message
        )
    }

    override fun error(message: () -> String) {
        log(
            level = LogLevel.ERROR,
            message = message
        )
    }

    private fun log(
        level: LogLevel,
        message: () -> String
    ) {
        val acceptingSinks: List<LogSink> = sinks().filter { sink: LogSink -> sink.accepts(level = level) }
        if (acceptingSinks.isNotEmpty()) {
            val text: String = message()
            acceptingSinks.forEach { sink: LogSink ->
                sink.write(
                    level = level,
                    tag = tag,
                    message = text
                )
            }
        }
    }
}