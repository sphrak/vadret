package se.vadretapp.vadret.pushalert

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.model.PushAlertType

// One alert of each type, worded like the server words them, so every icon and channel can be checked on a phone.
// Each type has its own notification id, so showing it again replaces only its own earlier notification.
internal val simulatedPushAlertMessages: ImmutableList<PushAlertMessage> = persistentListOf(
    PushAlertMessage(
        title = "Gul varning: Stockholms län",
        body = "Mycket kraftiga vindbyar",
        kind = PushAlertKind.WARNING,
        pushAlertType = PushAlertType.WARNING,
        notificationId = "simulated-warning",
        cell = null
    ),
    PushAlertMessage(
        title = "Pollen i Stockholm i dag",
        body = "Björk: höga. Gräs: låga.",
        kind = PushAlertKind.POLLEN,
        pushAlertType = PushAlertType.POLLEN,
        notificationId = "simulated-pollen",
        cell = null
    ),
    PushAlertMessage(
        title = "Varmare i morgon",
        body = "Högst 18° i morgon, mot 12° i dag.",
        kind = PushAlertKind.TEMPERATURE,
        pushAlertType = PushAlertType.WARMER,
        notificationId = "simulated-warmer",
        cell = null
    ),
    PushAlertMessage(
        title = "Kallare i morgon",
        body = "Högst 3° i morgon, mot 10° i dag.",
        kind = PushAlertKind.TEMPERATURE,
        pushAlertType = PushAlertType.COLDER,
        notificationId = "simulated-colder",
        cell = null
    ),
    PushAlertMessage(
        title = "Hård vind i morgon",
        body = "Upp till 16 m/s i medelvind runt kl. 14, 24 m/s i byarna.",
        kind = PushAlertKind.WIND,
        pushAlertType = PushAlertType.HARD_WIND,
        notificationId = "simulated-hard-wind",
        cell = null
    ),
    PushAlertMessage(
        title = "Storm i morgon",
        body = "Upp till 27 m/s i medelvind runt kl. 3, 36 m/s i byarna.",
        kind = PushAlertKind.WIND,
        pushAlertType = PushAlertType.STORM,
        notificationId = "simulated-storm",
        cell = null
    ),
    PushAlertMessage(
        title = "Orkan i morgon",
        body = "Upp till 34 m/s i medelvind runt kl. 5, 45 m/s i byarna.",
        kind = PushAlertKind.WIND,
        pushAlertType = PushAlertType.HURRICANE,
        notificationId = "simulated-hurricane",
        cell = null
    )
)