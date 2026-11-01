package se.vadretapp.vadret.logging

import kotlin.test.Test
import kotlin.test.assertEquals

class NsLogSinkTest {

    @Test
    fun `sink accepts its minimum level and up`() {
        val sink: LogSink = nsLogSink(minimumLevel = LogLevel.WARN)

        assertEquals(
            expected = listOf(false, false, true, true),
            actual = LogLevel.entries.map { level: LogLevel -> sink.accepts(level = level) }
        )
    }

    @Test
    fun `a line with format characters is written as text`() {
        nsLogSink(minimumLevel = LogLevel.DEBUG).write(
            level = LogLevel.WARN,
            tag = "se.vadretapp.vadret.logging",
            message = "Dropping 100% of %@ %s at path: ${'$'}.items[0]"
        )
    }
}