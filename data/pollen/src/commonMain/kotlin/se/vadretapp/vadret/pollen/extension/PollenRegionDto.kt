package se.vadretapp.vadret.pollen.extension

import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.pollen.model.PollenRegion
import se.vadretapp.vadret.pollen.remote.PollenRegionDto

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pollen")

internal fun PollenRegionDto.toPollenRegion(): PollenRegion? {
    val regionLatitude: Double? = latitude?.toDoubleOrNull()
    val regionLongitude: Double? = longitude?.toDoubleOrNull()
    return if (regionLatitude != null && regionLongitude != null) {
        PollenRegion(
            regionId = id,
            name = name,
            latitude = regionLatitude,
            longitude = regionLongitude
        )
    } else {
        logger.warn { "Skipping pollen region $id at latitude $latitude, longitude $longitude" }
        null
    }
}