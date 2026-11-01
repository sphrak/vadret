package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositeShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import se.vadretapp.vadret.radar.ui.map.BEAM_DEGREES_IN_CIRCLE
import se.vadretapp.vadret.radar.ui.map.RADAR_STATION_RANGE_KILOMETERS
import se.vadretapp.vadret.radar.ui.map.RadarStation
import se.vadretapp.vadret.radar.ui.map.beamDegrees
import se.vadretapp.vadret.radar.ui.map.radarStations
import kotlin.math.hypot

private const val KILOMETERS_PER_DEGREE_OF_LATITUDE = 111.32
private const val TRAIL_DEGREES = 48f
private const val LEADING_EDGE_DEGREES = 6f
private const val TRAIL_EASE_FRACTION = 0.35f
private const val NEAR_FRACTION = 0.2f
private const val STATION_DOT_RADIUS_DP = 1.5f
private const val UNIT_RADIUS = 1f
private const val UNIT_DIAMETER = 2f
private const val FIRST_STOP = 0f
private const val LAST_STOP = 1f
private const val NO_EXTENT = 0f

private val beamColor = Color(color = 0x59FFFFFF)
private val easedBeamColor = Color(color = 0x1FFFFFFF)
private val noColor = Color(color = 0x00FFFFFF)
private val opaqueMask = Color(color = 0xFFFFFFFF)
private val nearMask = Color(color = 0xD9FFFFFF)
private val stationDotColor = Color(color = 0xE6FFFFFF)
private val unitOvalTopLeft = Offset(x = -UNIT_RADIUS, y = -UNIT_RADIUS)
private val unitOvalSize = Size(width = UNIT_DIAMETER, height = UNIT_DIAMETER)

internal class StationBeamLayer {

    private val beamBrush = ShaderBrush(
        shader = CompositeShader(
            dst = SweepGradientShader(
                center = Offset.Zero,
                colors = listOf(beamColor, noColor, noColor, easedBeamColor, beamColor),
                colorStops = listOf(
                    FIRST_STOP,
                    LEADING_EDGE_DEGREES / BEAM_DEGREES_IN_CIRCLE,
                    (BEAM_DEGREES_IN_CIRCLE - TRAIL_DEGREES) / BEAM_DEGREES_IN_CIRCLE,
                    (BEAM_DEGREES_IN_CIRCLE - TRAIL_DEGREES * TRAIL_EASE_FRACTION) / BEAM_DEGREES_IN_CIRCLE,
                    LAST_STOP
                )
            ),
            src = RadialGradientShader(
                center = Offset.Zero,
                radius = UNIT_RADIUS,
                colors = listOf(opaqueMask, nearMask, noColor),
                colorStops = listOf(FIRST_STOP, NEAR_FRACTION, LAST_STOP),
                tileMode = TileMode.Clamp
            ),
            blendMode = BlendMode.DstIn
        )
    )

    fun DrawScope.drawBeams(
        screenProjection: ScreenProjection,
        timeMilliseconds: Long,
        opacity: Float
    ) {
        radarStations.forEachIndexed { stationIndex: Int, radarStation: RadarStation ->
            val station: Offset? = screenProjection.screenOffset(
                latitude = radarStation.latitude,
                longitude = radarStation.longitude
            )
            val range: Offset? = screenProjection.screenOffset(
                latitude = radarStation.latitude + RADAR_STATION_RANGE_KILOMETERS / KILOMETERS_PER_DEGREE_OF_LATITUDE,
                longitude = radarStation.longitude
            )
            if (station != null && range != null) {
                val radius: Float = hypot(x = range.x - station.x, y = range.y - station.y)
                if (isVisible(station = station, radius = radius)) {
                    drawBeam(
                        station = station,
                        radius = radius,
                        degrees = beamDegrees(
                            stationIndex = stationIndex,
                            timeMilliseconds = timeMilliseconds
                        ),
                        opacity = opacity
                    )
                    drawCircle(
                        color = stationDotColor,
                        radius = STATION_DOT_RADIUS_DP * density,
                        center = station,
                        alpha = stationDotColor.alpha * opacity
                    )
                }
            }
        }
    }

    private fun DrawScope.isVisible(
        station: Offset,
        radius: Float
    ): Boolean =
        station.x + radius > NO_EXTENT &&
            station.x - radius < size.width &&
            station.y + radius > NO_EXTENT &&
            station.y - radius < size.height

    private fun DrawScope.drawBeam(
        station: Offset,
        radius: Float,
        degrees: Float,
        opacity: Float
    ) {
        withTransform(
            transformBlock = {
                translate(
                    left = station.x,
                    top = station.y
                )
                rotate(
                    degrees = degrees,
                    pivot = Offset.Zero
                )
                scale(
                    scaleX = radius,
                    scaleY = radius,
                    pivot = Offset.Zero
                )
            }
        ) {
            drawArc(
                brush = beamBrush,
                startAngle = -TRAIL_DEGREES,
                sweepAngle = TRAIL_DEGREES + LEADING_EDGE_DEGREES,
                useCenter = true,
                topLeft = unitOvalTopLeft,
                size = unitOvalSize,
                alpha = opacity
            )
        }
    }
}