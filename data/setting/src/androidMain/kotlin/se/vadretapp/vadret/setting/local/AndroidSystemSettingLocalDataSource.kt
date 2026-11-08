package se.vadretapp.vadret.setting.local

import android.content.Context
import android.content.res.Resources
import android.text.format.DateFormat

internal class AndroidSystemSettingLocalDataSource(
    private val context: Context
) : SystemSettingLocalDataSource {

    override fun isTwentyFourHour(): Boolean = DateFormat.is24HourFormat(context)

    override fun languageTag(): String = Resources.getSystem().configuration.locales[0].toLanguageTag()
}