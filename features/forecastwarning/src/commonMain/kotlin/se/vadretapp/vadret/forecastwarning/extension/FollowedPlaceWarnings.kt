package se.vadretapp.vadret.forecastwarning.extension

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.warning.model.FollowedPlaceWarnings
import se.vadretapp.vadret.warning.model.PlaceWarning
import kotlin.time.Instant

private const val PLACE_NAME_SEPARATOR = ", "
private const val ONE_PLACE = 1

// With one followed place every warning is about it, so the places are only named once there are more.
internal fun FollowedPlaceWarnings.toWarningItems(
    now: Instant,
    timeZone: TimeZone,
    languageTag: String,
    hourFormat: HourFormat
): ImmutableList<WarningItem> =
    placeWarnings
        .sortedByDescending { placeWarning: PlaceWarning -> placeWarning.aggregatedFeed.publishedAt }
        .map { placeWarning: PlaceWarning ->
            placeWarning.aggregatedFeed.toWarningItem(
                now = now,
                timeZone = timeZone,
                languageTag = languageTag,
                hourFormat = hourFormat,
                followedPlaceNames = if (followedPlaces.size > ONE_PLACE) {
                    placeWarning.followedPlaces.placeNames()
                } else {
                    null
                }
            )
        }.toImmutableList()

private fun List<FollowedPlace>.placeNames(): String? =
    mapNotNull { followedPlace: FollowedPlace -> followedPlace.name }
        .joinToString(separator = PLACE_NAME_SEPARATOR)
        .ifEmpty { null }