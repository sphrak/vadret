package se.vadretapp.vadret.pollenpreference.extension

import se.vadretapp.vadret.pollenpreference.local.PollenPreferencesEntity
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences

private const val IS_POLLEN_ENABLED_BY_DEFAULT = true
private const val IS_HIBERNATION_NOTICE_SHOWN_BY_DEFAULT = false
private const val IS_END_OF_SEASON_SIMULATED_BY_DEFAULT = false

internal fun PollenPreferencesEntity.toPollenPreferences(): PollenPreferences =
    PollenPreferences(
        isPollenEnabled = isPollenEnabled ?: IS_POLLEN_ENABLED_BY_DEFAULT,
        allergenPollenIds = allergenPollenIds.orEmpty(),
        isHibernationNoticeShown = isHibernationNoticeShown ?: IS_HIBERNATION_NOTICE_SHOWN_BY_DEFAULT,
        isEndOfSeasonSimulated = isEndOfSeasonSimulated ?: IS_END_OF_SEASON_SIMULATED_BY_DEFAULT
    )