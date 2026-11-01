package se.vadretapp.vadret.logging

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.update

object LoggerFactory {

    private val sinks: AtomicReference<Map<String, LogSink>> = AtomicReference(value = emptyMap())

    fun register(
        name: String,
        sink: LogSink
    ) {
        sinks.update { registered: Map<String, LogSink> -> registered + (name to sink) }
    }

    fun unregister(name: String) {
        sinks.update { registered: Map<String, LogSink> -> registered - name }
    }

    fun getLogger(tag: String): Logger =
        DefaultLogger(
            tag = tag,
            sinks = { sinks.load().values }
        )
}