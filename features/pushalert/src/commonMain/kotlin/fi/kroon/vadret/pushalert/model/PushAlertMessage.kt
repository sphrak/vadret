package fi.kroon.vadret.pushalert.model

data class PushAlertMessage(
    val title: String,
    val body: String,
    val kind: PushAlertKind,
    val notificationId: String?
)