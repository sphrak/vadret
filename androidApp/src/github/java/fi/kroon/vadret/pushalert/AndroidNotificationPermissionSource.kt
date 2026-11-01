package fi.kroon.vadret.pushalert

import android.content.Context
import androidx.core.app.NotificationManagerCompat

internal class AndroidNotificationPermissionSource(
    private val context: Context
) : NotificationPermissionSource {
    override suspend fun isNotificationAllowed(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()
}