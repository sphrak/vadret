package se.vadretapp.vadret.pushalert.remote

import se.vadretapp.vadret.pushalert.model.ChallengeDto
import se.vadretapp.vadret.pushalert.model.DeviceRequestDto
import se.vadretapp.vadret.pushalert.model.InstallationDto
import se.vadretapp.vadret.pushalert.model.InstallationRequestDto
import se.vadretapp.vadret.pushalert.model.SubscriptionDto

internal interface PushAlertRemoteDataSource {
    suspend fun createChallenge(): ChallengeDto

    suspend fun install(installationRequestDto: InstallationRequestDto): InstallationDto

    suspend fun registerDevice(
        installationToken: String,
        deviceRequestDto: DeviceRequestDto
    )

    suspend fun replaceSubscription(
        installationToken: String,
        subscriptionDto: SubscriptionDto
    )

    suspend fun deleteInstallation(installationToken: String)
}