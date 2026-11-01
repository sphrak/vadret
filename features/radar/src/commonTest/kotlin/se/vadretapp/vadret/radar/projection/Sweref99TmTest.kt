package se.vadretapp.vadret.radar.projection

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

private const val TOLERANCE_METERS = 0.001

class Sweref99TmTest {

    private val gdalReferences: Map<Pair<Double, Double>, ProjectedPoint> = mapOf(
        (60.0 to 15.0) to ProjectedPoint(easting = 500000.000000001, northing = 6651411.1902397),
        (69.781 to 5.285) to ProjectedPoint(easting = 126646.761458179, northing = 7771241.73699872),
        (69.42 to 29.80) to ProjectedPoint(easting = 1075720.94147179, northing = 7771292.72309121),
        (53.869 to 9.319) to ProjectedPoint(easting = 126631.781318265, northing = 5983917.65503114),
        (53.686 to 23.727) to ProjectedPoint(easting = 1075675.60179392, northing = 5984030.88146621),
        (59.3293 to 18.0686) to ProjectedPoint(easting = 674571.866449145, northing = 6580743.00833094),
        (57.7 to 11.97) to ProjectedPoint(easting = 319439.772069869, northing = 6399348.061292),
        (67.8558 to 20.2253) to ProjectedPoint(easting = 719583.123005349, northing = 7536069.96992497),
        (70.05 to 15.0) to ProjectedPoint(easting = 500000.000000001, northing = 7771449.02116522),
        (53.9 to 15.0) to ProjectedPoint(easting = 500000.000000001, northing = 5972395.70693269)
    )

    @Test
    fun `projected points match gdal to the millimetre`() {
        val misses: Map<Pair<Double, Double>, ProjectedPoint> = gdalReferences.filter { (coordinate: Pair<Double, Double>, expected: ProjectedPoint) ->
            val actual: ProjectedPoint = Sweref99Tm.project(
                latitude = coordinate.first,
                longitude = coordinate.second
            )
            abs(actual.easting - expected.easting) > TOLERANCE_METERS || abs(actual.northing - expected.northing) > TOLERANCE_METERS
        }

        assertTrue(actual = misses.isEmpty(), message = "Off by more than a millimetre: $misses")
    }
}