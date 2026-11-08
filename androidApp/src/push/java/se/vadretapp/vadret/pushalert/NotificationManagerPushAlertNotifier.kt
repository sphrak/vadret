package se.vadretapp.vadret.pushalert

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import se.vadretapp.vadret.R
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.main.MainActivity
import se.vadretapp.vadret.main.putWarningReference
import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.model.PushAlertType
import android.R as AndroidR

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
        val notificationTag: String = pushAlertMessage.notificationId ?: pushAlertMessage.title
        val notification = NotificationCompat.Builder(context, pushAlertMessage.kind.channelId())
            .setSmallIcon(R.drawable.ic_push_alert)
            .setLargeIcon(largeIcon(pushAlertType = pushAlertMessage.pushAlertType))
            .setColor(ContextCompat.getColor(context, R.color.push_alert_accent))
            .setContentTitle(pushAlertMessage.title)
            .setContentText(pushAlertMessage.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(pushAlertMessage.body))
            .setContentIntent(
                openAppIntent(
                    pushAlertMessage = pushAlertMessage,
                    requestCode = notificationTag.hashCode()
                )
            )
            .setAutoCancel(true)
            .build()
        notificationManager.notify(
            pushAlertMessage.kind.channelId(),
            notificationTag.hashCode(),
            notification
        )
    }

    // The weather shows as a large coloured icon in the notification, while the small icon stays the app's own. An
    // alert of an unknown type shows no large icon.
    private fun largeIcon(pushAlertType: PushAlertType?): Bitmap? {
        if (pushAlertType == null) {
            return null
        }
        val drawable: Drawable? = ContextCompat.getDrawable(context, pushAlertType.largeIconResource())
        if (drawable == null) {
            return null
        }
        drawable.mutate().setTint(ContextCompat.getColor(context, pushAlertType.largeIconColorResource()))
        val largeIconSize: Int = context.resources.getDimensionPixelSize(AndroidR.dimen.notification_large_icon_width)
        return drawable.toBitmap(
            width = largeIconSize,
            height = largeIconSize
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
        ),
        notificationChannel(
            pushAlertKind = PushAlertKind.WIND,
            nameResource = R.string.push_alert_channel_wind,
            importance = NotificationManagerCompat.IMPORTANCE_DEFAULT
        ),
        notificationChannel(
            pushAlertKind = PushAlertKind.SLIPPERINESS,
            nameResource = R.string.push_alert_channel_slipperiness,
            importance = NotificationManagerCompat.IMPORTANCE_DEFAULT
        ),
        notificationChannel(
            pushAlertKind = PushAlertKind.AURORA,
            nameResource = R.string.push_alert_channel_aurora,
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

    private fun openAppIntent(
        pushAlertMessage: PushAlertMessage,
        requestCode: Int
    ): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        openAppActivityIntent(warningReference = pushAlertMessage.warningReference),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun openAppActivityIntent(warningReference: WarningReference?): Intent {
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return if (warningReference == null) {
            intent
        } else {
            intent.putWarningReference(warningReference = warningReference)
        }
    }
}

internal fun PushAlertType.largeIconResource(): Int =
    when (this) {
        PushAlertType.WARNING -> R.drawable.ic_push_alert_warning
        PushAlertType.POLLEN -> R.drawable.ic_push_alert_pollen
        PushAlertType.WARMER -> R.drawable.ic_push_alert_warmer
        PushAlertType.COLDER -> R.drawable.ic_push_alert_colder
        PushAlertType.HARD_WIND -> R.drawable.ic_push_alert_hard_wind
        PushAlertType.STORM -> R.drawable.ic_push_alert_storm
        PushAlertType.HURRICANE -> R.drawable.ic_push_alert_hurricane
        PushAlertType.SLIPPERINESS -> R.drawable.ic_push_alert_slipperiness
        PushAlertType.AURORA -> R.drawable.ic_push_alert_aurora
    }

internal fun PushAlertType.largeIconColorResource(): Int =
    when (this) {
        PushAlertType.WARNING -> R.color.push_alert_warning
        PushAlertType.POLLEN -> R.color.push_alert_pollen
        PushAlertType.WARMER -> R.color.push_alert_warmer
        PushAlertType.COLDER -> R.color.push_alert_colder
        PushAlertType.HARD_WIND,
        PushAlertType.STORM,
        PushAlertType.HURRICANE -> R.color.push_alert_wind
        PushAlertType.SLIPPERINESS -> R.color.push_alert_slipperiness
        PushAlertType.AURORA -> R.color.push_alert_aurora
    }

private fun PushAlertKind.channelId(): String =
    when (this) {
        PushAlertKind.WARNING -> WARNING_CHANNEL_ID
        PushAlertKind.POLLEN -> POLLEN_CHANNEL_ID
        PushAlertKind.TEMPERATURE -> TEMPERATURE_CHANNEL_ID
        PushAlertKind.WIND -> WIND_CHANNEL_ID
        PushAlertKind.SLIPPERINESS -> SLIPPERINESS_CHANNEL_ID
        PushAlertKind.AURORA -> AURORA_CHANNEL_ID
    }

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pushalert")

private const val WARNING_CHANNEL_ID = "warning"
private const val POLLEN_CHANNEL_ID = "pollen"
private const val TEMPERATURE_CHANNEL_ID = "temperature"
private const val WIND_CHANNEL_ID = "wind"
private const val SLIPPERINESS_CHANNEL_ID = "slipperiness"
private const val AURORA_CHANNEL_ID = "aurora"