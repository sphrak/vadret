package se.vadretapp.vadret.warning.extension

import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.warning.model.WarningPlace

internal fun FollowedPlace.toWarningPlace(): WarningPlace =
    WarningPlace(
        latitude = latitude,
        longitude = longitude,
        county = county,
        municipality = municipality
    )