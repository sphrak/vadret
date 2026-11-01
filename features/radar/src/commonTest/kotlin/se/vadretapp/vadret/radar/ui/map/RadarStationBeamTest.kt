package se.vadretapp.vadret.radar.ui.map

import se.vadretapp.vadret.radar.projection.RADAR_EAST_LONGITUDE
import se.vadretapp.vadret.radar.projection.RADAR_NORTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_SOUTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_WEST_LONGITUDE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SECOND_MILLISECONDS = 1_000L
private const val ANGLE_TOLERANCE = 0.001f

class RadarStationBeamTest {

    @Test
    fun `every radar station lies on the radar image`() {
        assertEquals(expected = 12, actual = radarStations.size)
        assertTrue(
            actual = radarStations.all { radarStation: RadarStation ->
                radarStation.latitude in RADAR_SOUTH_LATITUDE..RADAR_NORTH_LATITUDE &&
                    radarStation.longitude in RADAR_WEST_LONGITUDE..RADAR_EAST_LONGITUDE
            }
        )
    }

    @Test
    fun `no two beams point the same way at the same time`() {
        val degrees: List<Float> = radarStations.indices.map { stationIndex: Int ->
            beamDegrees(
                stationIndex = stationIndex,
                timeMilliseconds = SECOND_MILLISECONDS
            )
        }

        assertEquals(expected = degrees.size, actual = degrees.toSet().size)
    }

    @Test
    fun `stations turn at their own pace`() {
        val turnMilliseconds: List<Long> = radarStations.indices.map { stationIndex: Int ->
            beamTurnMilliseconds(stationIndex = stationIndex)
        }

        assertEquals(expected = turnMilliseconds.size, actual = turnMilliseconds.toSet().size)
        assertTrue(actual = turnMilliseconds.all { milliseconds: Long -> milliseconds in 14_000L until 18_800L })
    }

    @Test
    fun `a beam turns clockwise and comes back after one turn`() {
        val start: Float = beamDegrees(stationIndex = 0, timeMilliseconds = 0L)
        val later: Float = beamDegrees(stationIndex = 0, timeMilliseconds = SECOND_MILLISECONDS)
        val afterOneTurn: Float = beamDegrees(
            stationIndex = 0,
            timeMilliseconds = beamTurnMilliseconds(stationIndex = 0)
        )

        assertTrue(actual = later > start)
        assertEquals(expected = start, actual = afterOneTurn, absoluteTolerance = ANGLE_TOLERANCE)
    }
}