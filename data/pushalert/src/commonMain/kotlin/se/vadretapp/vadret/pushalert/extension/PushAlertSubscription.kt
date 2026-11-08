package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.HourFormatDto
import se.vadretapp.vadret.pushalert.model.LanguageDto
import se.vadretapp.vadret.pushalert.model.PollenSubscriptionDto
import se.vadretapp.vadret.pushalert.model.PushAlertSubscription
import se.vadretapp.vadret.pushalert.model.SubscriptionDto
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormat

internal fun PushAlertSubscription?.toSubscriptionDto(): SubscriptionDto =
    if (this == null) {
        SubscriptionDto()
    } else {
        SubscriptionDto(
            geohashes = geohashes.map { geohash -> geohash.value },
            warningLowestLevel = warningLowestLevel?.name,
            pollen = pollenChoices.map { pollenChoice ->
                PollenSubscriptionDto(
                    pollenTypeId = pollenChoice.pollenTypeId,
                    lowestLevel = pollenChoice.lowestLevel.level
                )
            },
            temperatureTrend = temperatureTrend,
            strongWind = strongWind,
            slipperiness = slipperiness,
            hourFormat = hourFormat.toHourFormatDto(),
            language = language.toLanguageDto()
        )
    }

private fun HourFormat.toHourFormatDto(): HourFormatDto =
    when (this) {
        HourFormat.TWENTY_FOUR_HOUR -> HourFormatDto.TWENTY_FOUR_HOUR
        HourFormat.TWELVE_HOUR -> HourFormatDto.TWELVE_HOUR
    }

// A sync sends the language the app shows, so the phone's own language is only ever left out, as an older app does.
private fun AppLanguage.toLanguageDto(): LanguageDto? =
    when (this) {
        AppLanguage.SYSTEM -> null
        AppLanguage.ENGLISH -> LanguageDto.ENGLISH
        AppLanguage.SWEDISH -> LanguageDto.SWEDISH
        AppLanguage.DANISH -> LanguageDto.DANISH
        AppLanguage.GERMAN -> LanguageDto.GERMAN
        AppLanguage.FINNISH -> LanguageDto.FINNISH
        AppLanguage.FRENCH -> LanguageDto.FRENCH
        AppLanguage.NORWEGIAN -> LanguageDto.NORWEGIAN
        AppLanguage.POLISH -> LanguageDto.POLISH
    }