package se.vadretapp.vadret.settings.extension

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import se.vadretapp.vadret.settings.Settings
import se.vadretapp.vadret.settings.model.LocationRow
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.setting_city
import se.vadretapp.vadret.ui.resources.setting_county
import se.vadretapp.vadret.ui.resources.setting_latitude
import se.vadretapp.vadret.ui.resources.setting_longitude
import se.vadretapp.vadret.ui.resources.setting_municipality

internal fun Settings.toLocationRows(): ImmutableList<LocationRow> =
    persistentListOf(
        LocationRow(titleResource = Res.string.setting_city, value = storedLocation.locality),
        LocationRow(titleResource = Res.string.setting_municipality, value = storedLocation.municipality),
        LocationRow(titleResource = Res.string.setting_county, value = storedLocation.county),
        LocationRow(titleResource = Res.string.setting_latitude, value = storedLocation.latitude.toString()),
        LocationRow(titleResource = Res.string.setting_longitude, value = storedLocation.longitude.toString())
    )