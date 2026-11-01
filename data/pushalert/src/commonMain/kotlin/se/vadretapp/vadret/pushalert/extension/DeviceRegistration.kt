package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.DeviceRegistration
import se.vadretapp.vadret.pushalert.model.DeviceRequestDto
import se.vadretapp.vadret.pushalert.model.DeviceToken

internal fun DeviceRegistration.toDeviceRequestDto(replacesDeviceToken: DeviceToken?): DeviceRequestDto = DeviceRequestDto(
    token = deviceToken.value,
    publicKey = devicePublicKey,
    replacesToken = replacesDeviceToken?.value
)