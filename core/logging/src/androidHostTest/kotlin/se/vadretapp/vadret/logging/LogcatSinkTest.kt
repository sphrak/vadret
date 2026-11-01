package se.vadretapp.vadret.logging

import kotlin.test.Test
import kotlin.test.assertEquals

class LogcatSinkTest {

    @Test
    fun `sink accepts its minimum level and up`() {
        val sink: LogSink = logcatSink(minimumLevel = LogLevel.WARN)

        assertEquals(
            expected = listOf(false, false, true, true),
            actual = LogLevel.entries.map { level: LogLevel -> sink.accepts(level = level) }
        )
    }
}