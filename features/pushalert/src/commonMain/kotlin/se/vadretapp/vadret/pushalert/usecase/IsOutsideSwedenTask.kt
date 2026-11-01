package se.vadretapp.vadret.pushalert.usecase

internal interface IsOutsideSwedenTask {
    suspend operator fun invoke(): Boolean
}