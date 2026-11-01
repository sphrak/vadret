package se.vadretapp.vadret.pushalert

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import se.vadretapp.vadret.R
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.main.MainActivity
import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage

internal class NotificationManagerPushAlertNotifier(
    private val context: Context
) : PushAlertNotifier {
    override fun show(pushAlertMessage: PushAlertMessage) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!isAllowedToNotify(notificationManager = notificationManager)) {
            logger.warn { "A push alert of kind ${pushAlertMessage.kind} is not shown because notifications are not allowed" }
            return
        }
        notificationManager.createNotificationChannelsCompat(notificationChannels())
        val notification = NotificationCompat.Builder(context, pushAlertMessage.kind.channelId())
            .setSmallIcon(R.drawable.ic_push_alert)
            .setContentTitle(pushAlertMessage.title)
            .setContentText(pushAlertMessage.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(pushAlertMessage.body))
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
        notificationManager.notify(
            pushAlertMessage.kind.channelId(),
            (pushAlertMessage.notificationId ?: pushAlertMessage.title).hashCode(),
            notification
        )
    }

    private fun isAllowedToNotify(notificationManager: NotificationManagerCompat): Boolean {
        if (!notificationManager.areNotificationsEnabled()) {
            return false
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun notificationChannels(): List<NotificationChannelCompat> = listOf(
        notificationChannel(
            pushAlertKind = PushAlertKind.WARNING,
            nameResource = R.string.push_alert_channel_warnings,
            importance = NotificationManagerCompat.IMPORTANCE_HIGH
        ),
        notificationChannel(
            pushAlertKind = PushAlertKind.POLLEN,
            nameResource = R.string.push_alert_channel_pollen,
            importance = NotificationManagerCompat.IMPORTANCE_DEFAULT
        ),
        notificationChannel(
            pushAlertKind = PushAlertKind.TEMPERATURE,
            nameResource = R.string.push_alert_channel_temperature,
            importance = NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
    )

    private fun notificationChannel(
        pushAlertKind: PushAlertKind,
        nameResource: Int,
        importance: Int
    ): NotificationChannelCompat = NotificationChannelCompat.Builder(pushAlertKind.channelId(), importance)
        .setName(context.getString(nameResource))
        .build()

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        OPEN_APP_REQUEST_CODE,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

private fun PushAlertKind.channelId(): String =
    when (this) {
        PushAlertKind.WARNING -> WARNING_CHANNEL_ID
        PushAlertKind.POLLEN -> POLLEN_CHANNEL_ID
        PushAlertKind.TEMPERATURE -> TEMPERATURE_CHANNEL_ID
    }

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pushalert")

private const val WARNING_CHANNEL_ID = "warning"
private const val POLLEN_CHANNEL_ID = "pollen"
private const val TEMPERATURE_CHANNEL_ID = "temperature"
private const val OPEN_APP_REQUEST_CODE = 0