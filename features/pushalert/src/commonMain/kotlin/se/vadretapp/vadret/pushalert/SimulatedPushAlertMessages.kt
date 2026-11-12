package se.vadretapp.vadret.pushalert

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.model.PushAlertType
import kotlin.time.Instant

// One alert of each type, worded like the server words them, so every icon and channel can be checked on a phone.
// Each type has its own notification id, so showing it again replaces only its own earlier notification.
internal fun simulatedPushAlertMessages(now: Instant): ImmutableList<PushAlertMessage> = persistentListOf(
    PushAlertMessage(
        title = "Gul varning: Stockholms län",
        body = "Mycket kraftiga vindbyar",
        kind = PushAlertKind.WARNING,
        pushAlertType = PushAlertType.WARNING,
        notificationId = "simulated-warning",
        day = null,
        cell = null,
        warningReference = null
    ),
    PushAlertMessage(
        title = "Pollen i Stockholm i dag",
        body = "Björk: höga. Gräs: låga.",
        kind = PushAlertKind.POLLEN,
        pushAlertType = PushAlertType.POLLEN,
        notificationId = "simulated-pollen",
        day = null,
        cell = null,
        warningReference = null
    ),
    PushAlertMessage(
        title = "Varmare i morgon - Stockholm",
        body = "Högst 18° i morgon, mot 12° i dag.",
        kind = PushAlertKind.TEMPERATURE,
        pushAlertType = PushAlertType.WARMER,
        notificationId = "simulated-warmer",
        day = null,
        cell = null,
        warningReference = null
    ),
    PushAlertMessage(
        title = "Kallare i morgon - Stockholm",
        body = "Högst 3° i morgon, mot 10° i dag.",
        kind = PushAlertKind.TEMPERATURE,
        pushAlertType = PushAlertType.COLDER,
        notificationId = "simulated-colder",
        day = null,
        cell = null,
        warningReference = null
    ),
    PushAlertMessage(
        title = "Hård vind i morgon - Stockholm",
        body = "Upp till 16 m/s i medelvind runt kl. 14:00, 24 m/s i byarna.",
        kind = PushAlertKind.WIND,
        pushAlertType = PushAlertType.HARD_WIND,
        notificationId = "simulated-hard-wind",
        day = null,
        cell = null,
        warningReference = null
    ),
    PushAlertMessage(
        title = "Storm i morgon - Stockholm",
        body = "Upp till 27 m/s i medelvind runt kl. 03:00, 36 m/s i byarna.",
        kind = PushAlertKind.WIND,
        pushAlertType = PushAlertType.STORM,
        notificationId = "simulated-storm",
        day = null,
        cell = null,
        warningReference = null
    ),
    PushAlertMessage(
        title = "Orkan i morgon - Stockholm",
        body = "Upp till 34 m/s i medelvind runt kl. 05:00, 45 m/s i byarna.",
        kind = PushAlertKind.WIND,
        pushAlertType = PushAlertType.HURRICANE,
        notificationId = "simulated-hurricane",
        day = null,
        cell = null,
        warningReference = null
    ),
    PushAlertMessage(
        title = "Risk för halka i natt - Stockholm",
        body = "Frost och återfrysning i natt runt kl. 01:00.",
        kind = PushAlertKind.SLIPPERINESS,
        pushAlertType = PushAlertType.SLIPPERINESS,
        notificationId = "simulated-slipperiness",
        day = null,
        cell = SIMULATED_SLIPPERINESS_CELL,
        warningReference = WarningReference.Slipperiness(
            geohash = Geohash(value = SIMULATED_SLIPPERINESS_CELL),
            slipperinessDay = now.swedishDayStartingAtNoon()
        )
    ),
    PushAlertMessage(
        title = "Stor chans för norrsken i kväll - Stockholm",
        body = "Norrsken kan förekomma mellan 20:00-01:00. Störst chans kl. 22:00.",
        kind = PushAlertKind.AURORA,
        pushAlertType = PushAlertType.AURORA,
        notificationId = "simulated-aurora",
        day = null,
        cell = SIMULATED_AURORA_CELL,
        warningReference = WarningReference.Aurora(
            geohash = Geohash(value = SIMULATED_AURORA_CELL),
            auroraNight = now.swedishDayStartingAtNoon()
        )
    ),
    simulatedDailyMessage(
        title = "4° i Stockholm",
        body = "Klart, 2° till 9°. Uppehåll hela dagen. Vindstyrka 4 m/s.",
        pushAlertType = PushAlertType.DAILY_CLEAR
    ),
    simulatedDailyMessage(
        title = "6° i Stockholm",
        body = "Halvklart, 3° till 8°. Uppehåll hela dagen. Vindstyrka 6 m/s.",
        pushAlertType = PushAlertType.DAILY_PARTLY_CLOUDY
    ),
    simulatedDailyMessage(
        title = "5° i Stockholm",
        body = "Molnigt, 4° till 6°. Uppehåll hela dagen. Vindstyrka 5 m/s.",
        pushAlertType = PushAlertType.DAILY_CLOUDY
    ),
    simulatedDailyMessage(
        title = "3° i Stockholm",
        body = "Molnigt, 2° till 7°. Regn från kl. 14:00, upp till 3 mm. Vindstyrka 8 m/s.",
        pushAlertType = PushAlertType.DAILY_RAIN
    ),
    simulatedDailyMessage(
        title = "-2° i Stockholm",
        body = "Molnigt, -4° till -1°. Snöfall från kl. 09:00, upp till 5 mm. Vindstyrka 6 m/s.",
        pushAlertType = PushAlertType.DAILY_SNOW
    ),
    simulatedDailyMessage(
        title = "1° i Stockholm",
        body = "Molnigt, 0° till 2°. Snöblandat regn från kl. 11:00, upp till 4 mm. Vindstyrka 7 m/s.",
        pushAlertType = PushAlertType.DAILY_SLEET
    ),
    simulatedDailyMessage(
        title = "19° i Stockholm",
        body = "Halvklart, 14° till 24°. Åska från kl. 16:00, upp till 12 mm. Vindstyrka 9 m/s.",
        pushAlertType = PushAlertType.DAILY_THUNDER
    )
)

private fun simulatedDailyMessage(
    title: String,
    body: String,
    pushAlertType: PushAlertType
): PushAlertMessage = PushAlertMessage(
    title = title,
    body = body,
    kind = PushAlertKind.DAILY,
    pushAlertType = pushAlertType,
    notificationId = "simulated-${pushAlertType.name.lowercase()}",
    day = null,
    cell = null,
    warningReference = null
)

private fun Instant.swedishDayStartingAtNoon(): String {
    val swedishTime: LocalDateTime = toLocalDateTime(timeZone = TimeZone.of(zoneId = SWEDISH_TIME_ZONE))
    return if (swedishTime.hour < NOON_HOUR) {
        swedishTime.date.minus(
            value = PREVIOUS_DAY,
            unit = DateTimeUnit.DAY
        )
    } else {
        swedishTime.date
    }.toString()
}

private const val SIMULATED_SLIPPERINESS_CELL = "u6sce"
private const val SIMULATED_AURORA_CELL = "u6sce"
private const val NOON_HOUR = 12
private const val PREVIOUS_DAY = 1
private const val SWEDISH_TIME_ZONE = "Europe/Stockholm"