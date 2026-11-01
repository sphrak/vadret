package fi.kroon.vadret.idgenerator

import kotlin.uuid.Uuid

interface IdGenerator {
    fun generate(): String
}

fun IdGenerator(): IdGenerator = DefaultIdGenerator()

internal class DefaultIdGenerator : IdGenerator {
    override fun generate(): String = Uuid.generateV7().toString()
}