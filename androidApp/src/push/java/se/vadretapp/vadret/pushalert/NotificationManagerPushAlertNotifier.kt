package se.vadretapp.vadret.pushalert

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import se.vadretapp.vadret.R
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.icons.VadretIconGraphic
import se.vadretapp.vadret.icons.drawVadretIcon
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.main.MainActivity
import se.vadretapp.vadret.main.putWarningReference
import se.vadretapp.vadret.pushalert.extension.icon
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
        if (isTodaysDailyForecastDismissed(
                pushAlertMessage = pushAlertMessage,
                notificationManager = notificationManager
            )
        ) {
            logger.debug { "Today's weather is not shown because it was dismissed today" }
            return
        }
        notificationManager.createNotificationChannelsCompat(notificationChannels())
        val notificationTag: String = pushAlertMessage.notificationTag()
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
            .setOnlyAlertOnce(true)
            .build()
        notificationManager.notify(
            pushAlertMessage.kind.channelId(),
            notificationTag.hashCode(),
            notification
        )
        rememberShownDailyForecast(pushAlertMessage = pushAlertMessage)
    }

    private fun isTodaysDailyForecastDismissed(
        pushAlertMessage: PushAlertMessage,
        notificationManager: NotificationManagerCompat
    ): Boolean {
        if (pushAlertMessage.kind != PushAlertKind.DAILY || pushAlertMessage.day == null) {
            return false
        }
        val isDailyForecastShowing: Boolean = notificationManager.activeNotifications.any { statusBarNotification ->
            statusBarNotification.tag == DAILY_CHANNEL_ID
        }
        return !isDailyForecastShowing && dailyForecastPreferences().getString(SHOWN_DAILY_FORECAST_DAY_KEY, null) == pushAlertMessage.day
    }

    private fun rememberShownDailyForecast(pushAlertMessage: PushAlertMessage) {
        if (pushAlertMessage.kind != PushAlertKind.DAILY || pushAlertMessage.day == null) {
            return
        }
        dailyForecastPreferences().edit {
            putString(SHOWN_DAILY_FORECAST_DAY_KEY, pushAlertMessage.day)
        }
    }

    private fun dailyForecastPreferences(): SharedPreferences = context.getSharedPreferences(DAILY_FORECAST_PREFERENCES_NAME, Context.MODE_PRIVATE)

    // The weather shows as a large coloured icon in the notification, while the small icon stays the app's own. An
    // alert of an unknown type shows no large icon.
    private fun largeIcon(pushAlertType: PushAlertType?): Bitmap? {
        if (pushAlertType == null) {
            return null
        }
        val largeIconSize: Int = context.resources.getDimensionPixelSize(AndroidR.dimen.notification_large_icon_width)
        val tint: Int = ContextCompat.getColor(context, pushAlertType.largeIconColorResource())
        val largeIconResource: Int = pushAlertType.largeIconResource() ?: return pushAlertType.icon().toBitmap(
            sizePixels = largeIconSize,
            tint = Color(color = tint)
        )
        val drawable: Drawable? = ContextCompat.getDrawable(context, largeIconResource)
        if (drawable == null) {
            return null
        }
        drawable.mutate().setTint(tint)
        return drawable.toBitmap(
            width = largeIconSize,
            height = largeIconSize
        )
    }

    private fun VadretIconGraphic.toBitmap(
        sizePixels: Int,
        tint: Color
    ): Bitmap {
        val imageBitmap = ImageBitmap(
            width = sizePixels,
            height = sizePixels
        )
        CanvasDrawScope().draw(
            density = Density(context = context),
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(image = imageBitmap),
            size = Size(
                width = sizePixels.toFloat(),
                height = sizePixels.toFloat()
            )
        ) {
            drawVadretIcon(
                icon = this@toBitmap,
                tint = tint
            )
        }
        return imageBitmap.asAndroidBitmap()
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
        ),
        notificationChannel(
            pushAlertKind = PushAlertKind.DAILY,
            nameResource = R.string.push_alert_channel_daily,
            importance = NotificationManagerCompat.IMPORTANCE_LOW
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

private fun PushAlertMessage.notificationTag(): String = when (kind) {
    PushAlertKind.DAILY -> DAILY_CHANNEL_ID
    else -> notificationId ?: title
}

internal fun PushAlertType.largeIconResource(): Int? =
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
        PushAlertType.DAILY_CLEAR,
        PushAlertType.DAILY_PARTLY_CLOUDY,
        PushAlertType.DAILY_CLOUDY,
        PushAlertType.DAILY_RAIN,
        PushAlertType.DAILY_SNOW,
        PushAlertType.DAILY_SLEET,
        PushAlertType.DAILY_THUNDER -> null
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
        PushAlertType.DAILY_CLEAR,
        PushAlertType.DAILY_PARTLY_CLOUDY,
        PushAlertType.DAILY_CLOUDY,
        PushAlertType.DAILY_RAIN,
        PushAlertType.DAILY_SNOW,
        PushAlertType.DAILY_SLEET,
        PushAlertType.DAILY_THUNDER -> R.color.push_alert_daily
    }

private fun PushAlertKind.channelId(): String =
    when (this) {
        PushAlertKind.WARNING -> WARNING_CHANNEL_ID
        PushAlertKind.POLLEN -> POLLEN_CHANNEL_ID
        PushAlertKind.TEMPERATURE -> TEMPERATURE_CHANNEL_ID
        PushAlertKind.WIND -> WIND_CHANNEL_ID
        PushAlertKind.SLIPPERINESS -> SLIPPERINESS_CHANNEL_ID
        PushAlertKind.AURORA -> AURORA_CHANNEL_ID
        PushAlertKind.DAILY -> DAILY_CHANNEL_ID
    }

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pushalert")

private const val WARNING_CHANNEL_ID = "warning"
private const val POLLEN_CHANNEL_ID = "pollen"
private const val TEMPERATURE_CHANNEL_ID = "temperature"
private const val WIND_CHANNEL_ID = "wind"
private const val SLIPPERINESS_CHANNEL_ID = "slipperiness"
private const val AURORA_CHANNEL_ID = "aurora"
private const val DAILY_CHANNEL_ID = "daily"
private const val DAILY_FORECAST_PREFERENCES_NAME = "push_alert_daily"
private const val SHOWN_DAILY_FORECAST_DAY_KEY = "shown_daily_day"