package se.vadretapp.vadret.pushalert.model

import se.vadretapp.vadret.aggregatedfeed.model.WarningReference

data class PushAlertMessage(
    val title: String,
    val body: String,
    val kind: PushAlertKind,
    val pushAlertType: PushAlertType?,
    val notificationId: String?,
    val day: String?,
    val cell: String?,
    val warningReference: WarningReference?
)