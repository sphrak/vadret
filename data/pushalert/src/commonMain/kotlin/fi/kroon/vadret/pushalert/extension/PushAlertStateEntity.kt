package fi.kroon.vadret.pushalert.extension

import fi.kroon.vadret.pushalert.model.DeviceRegistration
import fi.kroon.vadret.pushalert.model.DeviceToken
import fi.kroon.vadret.pushalert.model.InstallationToken
import fi.kroon.vadret.pushalert.model.PushAlertStateEntity

internal fun PushAlertStateEntity.toInstallationToken(): InstallationToken? {
    val installationToken = installationToken ?: return null
    return InstallationToken(value = installationToken)
}

internal fun PushAlertStateEntity.toDeviceRegistration(): DeviceRegistration? {
    val registeredDeviceToken = registeredDeviceToken ?: return null
    val registeredDevicePublicKey = registeredDevicePublicKey ?: return null
    val registeredAtMillis = registeredAtMillis ?: return null
    return DeviceRegistration(
        deviceToken = DeviceToken(value = registeredDeviceToken),
        devicePublicKey = registeredDevicePublicKey,
        registeredAtMillis = registeredAtMillis
    )
}

internal fun PushAlertStateEntity.withInstallationToken(installationToken: InstallationToken): PushAlertStateEntity =
    withoutInstallation().copy(installationToken = installationToken.value)

internal fun PushAlertStateEntity.withDeviceRegistration(deviceRegistration: DeviceRegistration): PushAlertStateEntity = copy(
    registeredDeviceToken = deviceRegistration.deviceToken.value,
    registeredDevicePublicKey = deviceRegistration.devicePublicKey,
    registeredAtMillis = deviceRegistration.registeredAtMillis
)

internal fun PushAlertStateEntity.withoutInstallation(): PushAlertStateEntity = PushAlertStateEntity(
    choices = choices,
    isSyncPending = isSyncPending
)