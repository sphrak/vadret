package se.vadretapp.vadret.setting.local

internal interface SystemSettingLocalDataSource {
    fun isTwentyFourHour(): Boolean
    fun languageTag(): String
}