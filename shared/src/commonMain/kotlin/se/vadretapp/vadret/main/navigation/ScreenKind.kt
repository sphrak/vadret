package se.vadretapp.vadret.main.navigation

internal enum class ScreenKind {
    TAB,
    DETAIL,
    DETAIL_WITH_OWN_HEADER,
    SHEET,
    STORY
}

internal val VadretKey.screenKind: ScreenKind
    get() = when (this) {
        WeatherKey -> ScreenKind.TAB
        PlaceSearchKey -> ScreenKind.SHEET
        WarningKey -> ScreenKind.TAB
        RadarKey -> ScreenKind.TAB
        SettingsKey -> ScreenKind.TAB
        is PlaceForecastKey -> ScreenKind.DETAIL_WITH_OWN_HEADER
        is WarningDetailKey -> ScreenKind.DETAIL_WITH_OWN_HEADER
        WarningPlacesKey -> ScreenKind.SHEET
        is WarningStoryKey -> ScreenKind.STORY
        is SimulatedWarningStoryKey -> ScreenKind.STORY
        SettingsPlacesKey -> ScreenKind.DETAIL
        is SettingsSectionKey -> ScreenKind.DETAIL
        SettingsPollenKey -> ScreenKind.DETAIL
        SettingsLabsKey -> ScreenKind.DETAIL
        SettingsAboutKey -> ScreenKind.DETAIL
        SettingsLicensesKey -> ScreenKind.DETAIL
        SettingsDeveloperKey -> ScreenKind.DETAIL
        is SettingsDeveloperSectionKey -> ScreenKind.DETAIL
    }