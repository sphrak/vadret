package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.model.PushAlertType
import se.vadretapp.vadret.pushalert.model.SealedContentDto
import se.vadretapp.vadret.pushalert.pushAlertWarningReferenceOrNull

internal fun SealedContentDto.toPushAlertMessageOrNull(): PushAlertMessage? {
    val title = title ?: return null
    val body = body ?: return null
    val data = data ?: return null
    val pushAlertKind = data[KIND_FIELD]?.toPushAlertKindOrNull() ?: return null
    return PushAlertMessage(
        title = title,
        body = body,
        kind = pushAlertKind,
        pushAlertType = data[ALERT_TYPE_FIELD]?.toPushAlertTypeOrNull(),
        notificationId = data[NOTIFICATION_ID_FIELD],
        day = data[DAY_FIELD],
        cell = data[CELL_FIELD],
        warningReference = pushAlertWarningReferenceOrNull(pushAlertData = data)
    )
}

private fun String.toPushAlertKindOrNull(): PushAlertKind? =
    when (this) {
        WARNING_KIND -> PushAlertKind.WARNING
        POLLEN_KIND -> PushAlertKind.POLLEN
        TEMPERATURE_KIND -> PushAlertKind.TEMPERATURE
        WIND_KIND -> PushAlertKind.WIND
        SLIPPERINESS_KIND -> PushAlertKind.SLIPPERINESS
        AURORA_KIND -> PushAlertKind.AURORA
        DAILY_KIND -> PushAlertKind.DAILY
        else -> null
    }

private fun String.toPushAlertTypeOrNull(): PushAlertType? =
    when (this) {
        WARNING_TYPE -> PushAlertType.WARNING
        POLLEN_TYPE -> PushAlertType.POLLEN
        WARMER_TYPE -> PushAlertType.WARMER
        COLDER_TYPE -> PushAlertType.COLDER
        HARD_WIND_TYPE -> PushAlertType.HARD_WIND
        STORM_TYPE -> PushAlertType.STORM
        HURRICANE_TYPE -> PushAlertType.HURRICANE
        SLIPPERINESS_TYPE -> PushAlertType.SLIPPERINESS
        AURORA_TYPE -> PushAlertType.AURORA
        DAILY_CLEAR_TYPE -> PushAlertType.DAILY_CLEAR
        DAILY_PARTLY_CLOUDY_TYPE -> PushAlertType.DAILY_PARTLY_CLOUDY
        DAILY_CLOUDY_TYPE -> PushAlertType.DAILY_CLOUDY
        DAILY_RAIN_TYPE -> PushAlertType.DAILY_RAIN
        DAILY_SNOW_TYPE -> PushAlertType.DAILY_SNOW
        DAILY_SLEET_TYPE -> PushAlertType.DAILY_SLEET
        DAILY_THUNDER_TYPE -> PushAlertType.DAILY_THUNDER
        DAILY_STORM_TYPE -> PushAlertType.DAILY_STORM
        else -> null
    }

private const val KIND_FIELD = "kind"
private const val ALERT_TYPE_FIELD = "alert_type"
private const val NOTIFICATION_ID_FIELD = "notification_id"
private const val DAY_FIELD = "day"
private const val CELL_FIELD = "cell"
private const val WARNING_KIND = "warning"
private const val POLLEN_KIND = "pollen"
private const val TEMPERATURE_KIND = "temperature"
private const val WIND_KIND = "wind"
private const val SLIPPERINESS_KIND = "slipperiness"
private const val AURORA_KIND = "aurora"
private const val DAILY_KIND = "daily"
private const val WARNING_TYPE = "warning"
private const val POLLEN_TYPE = "pollen"
private const val WARMER_TYPE = "warmer"
private const val COLDER_TYPE = "colder"
private const val HARD_WIND_TYPE = "hard_wind"
private const val STORM_TYPE = "storm"
private const val HURRICANE_TYPE = "hurricane"
private const val SLIPPERINESS_TYPE = "slipperiness"
private const val AURORA_TYPE = "aurora"
private const val DAILY_CLEAR_TYPE = "daily_clear"
private const val DAILY_PARTLY_CLOUDY_TYPE = "daily_partly_cloudy"
private const val DAILY_CLOUDY_TYPE = "daily_cloudy"
private const val DAILY_RAIN_TYPE = "daily_rain"
private const val DAILY_SNOW_TYPE = "daily_snow"
private const val DAILY_SLEET_TYPE = "daily_sleet"
private const val DAILY_THUNDER_TYPE = "daily_thunder"
private const val DAILY_STORM_TYPE = "daily_storm"