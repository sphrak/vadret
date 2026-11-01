package se.vadretapp.vadret.radar.projection

import se.vadretapp.vadret.radar.extension.toRadians
import kotlin.math.atan
import kotlin.math.atanh
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan

private const val SEMI_MAJOR_AXIS = 6378137.0
private const val FLATTENING = 1.0 / 298.257222101
private const val CENTRAL_MERIDIAN_DEGREES = 15.0
private const val SCALE_FACTOR = 0.9996
private const val FALSE_EASTING = 500000.0
private const val FALSE_NORTHING = 0.0

private val eccentricitySquared: Double = FLATTENING * (2.0 - FLATTENING)
private val thirdFlattening: Double = FLATTENING / (2.0 - FLATTENING)
private val rectifyingRadius: Double = SEMI_MAJOR_AXIS / (1.0 + thirdFlattening) *
    (1.0 + thirdFlattening.pow(2) / 4.0 + thirdFlattening.pow(4) / 64.0)

private val latitudeA: Double = eccentricitySquared
private val latitudeB: Double = (5.0 * eccentricitySquared.pow(2) - eccentricitySquared.pow(3)) / 6.0
private val latitudeC: Double = (104.0 * eccentricitySquared.pow(3) - 45.0 * eccentricitySquared.pow(4)) / 120.0
private val latitudeD: Double = 1237.0 * eccentricitySquared.pow(4) / 1260.0

private val beta1: Double = thirdFlattening / 2.0 - 2.0 * thirdFlattening.pow(2) / 3.0 +
    5.0 * thirdFlattening.pow(3) / 16.0 + 41.0 * thirdFlattening.pow(4) / 180.0
private val beta2: Double = 13.0 * thirdFlattening.pow(2) / 48.0 - 3.0 * thirdFlattening.pow(3) / 5.0 +
    557.0 * thirdFlattening.pow(4) / 1440.0
private val beta3: Double = 61.0 * thirdFlattening.pow(3) / 240.0 - 103.0 * thirdFlattening.pow(4) / 140.0
private val beta4: Double = 49561.0 * thirdFlattening.pow(4) / 161280.0

private val centralMeridian: Double = CENTRAL_MERIDIAN_DEGREES.toRadians()

internal object Sweref99Tm {

    fun project(
        latitude: Double,
        longitude: Double
    ): ProjectedPoint {
        val geodeticLatitude: Double = latitude.toRadians()
        val sinLatitude: Double = sin(geodeticLatitude)
        val sinLatitudeSquared: Double = sinLatitude * sinLatitude
        val conformalLatitude: Double = geodeticLatitude - sinLatitude * cos(geodeticLatitude) *
            (latitudeA + latitudeB * sinLatitudeSquared + latitudeC * sinLatitudeSquared.pow(2) + latitudeD * sinLatitudeSquared.pow(3))
        val longitudeFromCentralMeridian: Double = longitude.toRadians() - centralMeridian
        val xi: Double = atan(tan(conformalLatitude) / cos(longitudeFromCentralMeridian))
        val eta: Double = atanh(cos(conformalLatitude) * sin(longitudeFromCentralMeridian))
        val northing: Double = SCALE_FACTOR * rectifyingRadius * (
            xi +
                beta1 * sin(2.0 * xi) * cosh(2.0 * eta) +
                beta2 * sin(4.0 * xi) * cosh(4.0 * eta) +
                beta3 * sin(6.0 * xi) * cosh(6.0 * eta) +
                beta4 * sin(8.0 * xi) * cosh(8.0 * eta)
            ) + FALSE_NORTHING
        val easting: Double = SCALE_FACTOR * rectifyingRadius * (
            eta +
                beta1 * cos(2.0 * xi) * sinh(2.0 * eta) +
                beta2 * cos(4.0 * xi) * sinh(4.0 * eta) +
                beta3 * cos(6.0 * xi) * sinh(6.0 * eta) +
                beta4 * cos(8.0 * xi) * sinh(8.0 * eta)
            ) + FALSE_EASTING
        return ProjectedPoint(
            easting = easting,
            northing = northing
        )
    }
}