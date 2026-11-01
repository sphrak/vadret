package se.vadretapp.vadret.logging

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class ListSink(private val minimumLevel: LogLevel) : LogSink {

    val lines: MutableList<String> = mutableListOf()

    override fun accepts(level: LogLevel): Boolean = level >= minimumLevel

    override fun write(
        level: LogLevel,
        tag: String,
        message: String
    ) {
        lines.add("$level $tag $message")
    }
}

class LoggerFactoryTest {

    private val sinkName: String = "logger-factory-test"
    private val tag: String = "se.vadretapp.vadret.logging"

    @AfterTest
    fun tearDown() {
        LoggerFactory.unregister(name = sinkName)
    }

    @Test
    fun `every level reaches a sink that accepts it`() {
        val sink = ListSink(minimumLevel = LogLevel.DEBUG)
        LoggerFactory.register(name = sinkName, sink = sink)
        val logger: Logger = LoggerFactory.getLogger(tag = tag)

        logger.debug { "one" }
        logger.info { "two" }
        logger.warn { "three" }
        logger.error { "four" }

        assertEquals(
            expected = listOf(
                "DEBUG $tag one",
                "INFO $tag two",
                "WARN $tag three",
                "ERROR $tag four"
            ),
            actual = sink.lines
        )
    }

    @Test
    fun `a message nobody accepts is never built`() {
        val sink = ListSink(minimumLevel = LogLevel.ERROR)
        LoggerFactory.register(name = sinkName, sink = sink)
        var isBuilt = false

        LoggerFactory.getLogger(tag = tag).warn {
            isBuilt = true
            "ignored"
        }

        assertFalse(actual = isBuilt)
        assertTrue(actual = sink.lines.isEmpty())
    }

    @Test
    fun `a logger created before a sink is registered writes to it`() {
        val logger: Logger = LoggerFactory.getLogger(tag = tag)
        val sink = ListSink(minimumLevel = LogLevel.DEBUG)
        LoggerFactory.register(name = sinkName, sink = sink)

        logger.info { "late" }

        assertEquals(expected = listOf("INFO $tag late"), actual = sink.lines)
    }

    @Test
    fun `an unregistered sink gets nothing more`() {
        val sink = ListSink(minimumLevel = LogLevel.DEBUG)
        LoggerFactory.register(name = sinkName, sink = sink)
        val logger: Logger = LoggerFactory.getLogger(tag = tag)

        LoggerFactory.unregister(name = sinkName)
        logger.error { "gone" }

        assertTrue(actual = sink.lines.isEmpty())
    }
}