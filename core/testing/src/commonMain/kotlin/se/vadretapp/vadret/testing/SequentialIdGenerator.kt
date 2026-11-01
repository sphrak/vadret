package se.vadretapp.vadret.testing

import se.vadretapp.vadret.idgenerator.IdGenerator
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.fetchAndIncrement

class SequentialIdGenerator(
    private val ids: List<String>
) : IdGenerator {

    private val nextIndex: AtomicInt = AtomicInt(value = 0)

    override fun generate(): String = ids[nextIndex.fetchAndIncrement()]
}