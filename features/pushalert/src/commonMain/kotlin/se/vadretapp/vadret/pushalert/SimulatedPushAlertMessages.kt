package se.vadretapp.vadret.pushalert

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.model.PushAlertType
import kotlin.time.Duration.Companion.hours
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
        cell = null,
        warningReference = null
    ),
    PushAlertMessage(
        title = "Pollen i Stockholm i dag",
        body = "Björk: höga. Gräs: låga.",
        kind = PushAlertKind.POLLEN,
        pushAlertType = PushAlertType.POLLEN,
        notificationId = "simulated-pollen",
        cell = null,
        warningReference = null
    ),
    PushAlertMessage(
        title = "Varmare i morgon",
        body = "Högst 18° i morgon, mot 12° i dag.",
        kind = PushAlertKind.TEMPERATURE,
        pushAlertType = PushAlertType.WARMER,
        notificationId = "simulated-warmer",
        cell = null,
        warningReference = null
    ),
    PushAlertMessage(
        title = "Kallare i morgon",
        body = "Högst 3° i morgon, mot 10° i dag.",
        kind = PushAlertKind.TEMPERATURE,
        pushAlertType = PushAlertType.COLDER,
        notificationId = "simulated-colder",
        cell = null,
        warningReference = null
    ),
    PushAlertMessage(
        title = "Hård vind i morgon",
        body = "Upp till 16 m/s i medelvind runt kl. 14, 24 m/s i byarna.",
        kind = PushAlertKind.WIND,
        pushAlertType = PushAlertType.HARD_WIND,
        notificationId = "simulated-hard-wind",
        cell = null,
        warningReference = null
    ),
    PushAlertMessage(
        title = "Storm i morgon",
        body = "Upp till 27 m/s i medelvind runt kl. 3, 36 m/s i byarna.",
        kind = PushAlertKind.WIND,
        pushAlertType = PushAlertType.STORM,
        notificationId = "simulated-storm",
        cell = null,
        warningReference = null
    ),
    PushAlertMessage(
        title = "Orkan i morgon",
        body = "Upp till 34 m/s i medelvind runt kl. 5, 45 m/s i byarna.",
        kind = PushAlertKind.WIND,
        pushAlertType = PushAlertType.HURRICANE,
        notificationId = "simulated-hurricane",
        cell = null,
        warningReference = null
    ),
    PushAlertMessage(
        title = "Möjlig halka i natt",
        body = "Rimfrost och återfrysning, ungefär kl. 23-06.",
        kind = PushAlertKind.SLIPPERINESS,
        pushAlertType = PushAlertType.SLIPPERINESS,
        notificationId = "simulated-slipperiness",
        cell = SIMULATED_SLIPPERINESS_CELL,
        warningReference = WarningReference.Slipperiness(
            geohash = Geohash(value = SIMULATED_SLIPPERINESS_CELL),
            slipperinessDay = now.slipperinessDay()
        )
    )
)

private fun Instant.slipperinessDay(): String =
    minus(duration = SLIPPERINESS_DAY_OFFSET_HOURS.hours)
        .toLocalDateTime(timeZone = TimeZone.of(zoneId = SWEDISH_TIME_ZONE))
        .date
        .toString()

private const val SIMULATED_SLIPPERINESS_CELL = "u6sce"
private const val SLIPPERINESS_DAY_OFFSET_HOURS = 12
private const val SWEDISH_TIME_ZONE = "Europe/Stockholm"