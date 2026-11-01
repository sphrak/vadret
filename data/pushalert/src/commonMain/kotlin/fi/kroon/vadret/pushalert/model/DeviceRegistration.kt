package fi.kroon.vadret.pushalert.model

data class DeviceRegistration(
    val deviceToken: DeviceToken,
    val devicePublicKey: String,
    val registeredAtMillis: Long
)