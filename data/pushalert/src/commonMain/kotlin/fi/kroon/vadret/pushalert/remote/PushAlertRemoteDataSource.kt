package fi.kroon.vadret.pushalert.remote

import fi.kroon.vadret.pushalert.model.ChallengeDto
import fi.kroon.vadret.pushalert.model.DeviceRequestDto
import fi.kroon.vadret.pushalert.model.InstallationDto
import fi.kroon.vadret.pushalert.model.InstallationRequestDto
import fi.kroon.vadret.pushalert.model.SubscriptionDto

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