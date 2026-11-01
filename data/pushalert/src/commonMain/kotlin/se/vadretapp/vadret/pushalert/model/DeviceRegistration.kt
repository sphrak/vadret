package se.vadretapp.vadret.pushalert.model

data class DeviceRegistration(
    val deviceToken: DeviceToken,
    val devicePublicKey: String,
    val registeredAtMillis: Long
)