package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.model.SealedContentDto

internal fun SealedContentDto.toPushAlertMessageOrNull(): PushAlertMessage? {
    val title = title ?: return null
    val body = body ?: return null
    val data = data ?: return null
    val pushAlertKind = data[KIND_FIELD]?.toPushAlertKindOrNull() ?: return null
    return PushAlertMessage(
        title = title,
        body = body,
        kind = pushAlertKind,
        notificationId = data[NOTIFICATION_ID_FIELD]
    )
}

private fun String.toPushAlertKindOrNull(): PushAlertKind? =
    when (this) {
        WARNING_KIND -> PushAlertKind.WARNING
        POLLEN_KIND -> PushAlertKind.POLLEN
        TEMPERATURE_KIND -> PushAlertKind.TEMPERATURE
        else -> null
    }

private const val KIND_FIELD = "kind"
private const val NOTIFICATION_ID_FIELD = "notification_id"
private const val WARNING_KIND = "warning"
private const val POLLEN_KIND = "pollen"
private const val TEMPERATURE_KIND = "temperature"