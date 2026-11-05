package se.vadretapp.vadret.location.extension

import se.vadretapp.vadret.location.local.LocationPreferenceEntity
import se.vadretapp.vadret.location.model.LocationPreference

internal fun LocationPreferenceEntity.toLocationPreference(): LocationPreference =
    LocationPreference(
        isAutomatic = isAutomatic,
        storedLocation = storedLocationEntity.toStoredLocation()
    )