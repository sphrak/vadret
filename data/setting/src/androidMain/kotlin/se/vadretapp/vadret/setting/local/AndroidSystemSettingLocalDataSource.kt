package se.vadretapp.vadret.setting.local

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import se.vadretapp.vadret.setting.model.SystemSettingsEntity

internal class AndroidSystemSettingLocalDataSource(
    private val context: Context
) : SystemSettingLocalDataSource {

    override fun getSystemSettings(): SystemSettingsEntity = SystemSettingsEntity(
        isTwentyFourHour = DateFormat.is24HourFormat(context),
        languageTag = Resources.getSystem().configuration.locales[0].toLanguageTag()
    )

    // Android tells the app when the clock or the language of the phone is switched, also while the app runs.
    override fun observeSystemSettings(): Flow<SystemSettingsEntity> =
        callbackFlow {
            val receiver = SystemSettingsReceiver(onChange = { trySend(element = getSystemSettings()) })
            val intentFilter = IntentFilter(Intent.ACTION_TIME_CHANGED)
            intentFilter.addAction(Intent.ACTION_LOCALE_CHANGED)
            ContextCompat.registerReceiver(
                context,
                receiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            trySend(element = getSystemSettings())
            awaitClose { context.unregisterReceiver(receiver) }
        }.distinctUntilChanged()
}

private class SystemSettingsReceiver(
    private val onChange: () -> Unit
) : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        onChange()
    }
}