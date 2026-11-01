package fi.kroon.vadret.pushalert.extension

import fi.kroon.vadret.pushalert.model.DeviceRegistration
import fi.kroon.vadret.pushalert.model.DeviceRequestDto
import fi.kroon.vadret.pushalert.model.DeviceToken

internal fun DeviceRegistration.toDeviceRequestDto(replacesDeviceToken: DeviceToken?): DeviceRequestDto = DeviceRequestDto(
    token = deviceToken.value,
    publicKey = devicePublicKey,
    replacesToken = replacesDeviceToken?.value
)