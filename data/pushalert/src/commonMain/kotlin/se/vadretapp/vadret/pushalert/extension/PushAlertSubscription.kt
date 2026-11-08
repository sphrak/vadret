package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.HourFormatDto
import se.vadretapp.vadret.pushalert.model.LanguageDto
import se.vadretapp.vadret.pushalert.model.PollenSubscriptionDto
import se.vadretapp.vadret.pushalert.model.PushAlertSubscription
import se.vadretapp.vadret.pushalert.model.SubscriptionDto
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.Language

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

private fun Language.toLanguageDto(): LanguageDto =
    when (this) {
        Language.ENGLISH -> LanguageDto.ENGLISH
        Language.SWEDISH -> LanguageDto.SWEDISH
        Language.DANISH -> LanguageDto.DANISH
        Language.GERMAN -> LanguageDto.GERMAN
        Language.FINNISH -> LanguageDto.FINNISH
        Language.FRENCH -> LanguageDto.FRENCH
        Language.NORWEGIAN -> LanguageDto.NORWEGIAN
        Language.POLISH -> LanguageDto.POLISH
    }