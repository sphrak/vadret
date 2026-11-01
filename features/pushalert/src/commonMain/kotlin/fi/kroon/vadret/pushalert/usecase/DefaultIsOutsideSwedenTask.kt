package fi.kroon.vadret.pushalert.usecase

import io.github.sphrak.either.Either

internal class DefaultIsOutsideSwedenTask(
    private val getCurrentGeohashTask: GetCurrentGeohashTask
) : IsOutsideSwedenTask {
    override suspend fun invoke(): Boolean =
        when (val currentGeohash = getCurrentGeohashTask()) {
            is Either.Left -> false
            is Either.Right -> !currentGeohash.b.isInSwedishWaters()
        }
}