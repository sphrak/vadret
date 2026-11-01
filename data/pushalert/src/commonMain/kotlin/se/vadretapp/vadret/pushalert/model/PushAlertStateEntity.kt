package se.vadretapp.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PushAlertStateEntity(
    @SerialName(value = "installation_token")
    val installationToken: String? = null,
    @SerialName(value = "registered_device_token")
    val registeredDeviceToken: String? = null,
    @SerialName(value = "registered_device_public_key")
    val registeredDevicePublicKey: String? = null,
    @SerialName(value = "registered_at_millis")
    val registeredAtMillis: Long? = null,
    @SerialName(value = "synced_current_geohash")
    val syncedCurrentGeohash: String? = null,
    @SerialName(value = "choices")
    val choices: PushAlertChoicesEntity = PushAlertChoicesEntity(),
    @SerialName(value = "is_sync_pending")
    val isSyncPending: Boolean = false
)