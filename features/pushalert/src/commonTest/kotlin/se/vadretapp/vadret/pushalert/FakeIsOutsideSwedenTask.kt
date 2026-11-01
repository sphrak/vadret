package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.pushalert.usecase.IsOutsideSwedenTask

internal class FakeIsOutsideSwedenTask(
    private val isOutsideSweden: Boolean = false
) : IsOutsideSwedenTask {
    var checks = 0

    override suspend fun invoke(): Boolean {
        checks++
        return isOutsideSweden
    }
}