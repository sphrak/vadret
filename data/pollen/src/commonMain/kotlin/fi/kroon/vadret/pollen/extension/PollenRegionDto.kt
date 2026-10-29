package fi.kroon.vadret.pollen.extension

import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import fi.kroon.vadret.pollen.model.PollenRegion
import fi.kroon.vadret.pollen.remote.PollenRegionDto

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.pollen")

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