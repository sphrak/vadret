package se.vadretapp.vadret.radar.ui.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Projection
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val KILOMETERS_PER_DEGREE_OF_LATITUDE = 111.32
private const val DEGREES_IN_CIRCLE = 360f
private const val TRAIL_DEGREES = 48f
private const val LEADING_EDGE_DEGREES = 6f
private const val TRAIL_EASE_FRACTION = 0.35f
private const val BEAM_COLOR = 0x59FFFFFF
private const val EASED_BEAM_COLOR = 0x1FFFFFFF
private const val NO_COLOR = 0x00FFFFFF
private const val OPAQUE_MASK = 0xFFFFFFFF.toInt()
private const val NEAR_MASK = 0xD9FFFFFF.toInt()
private const val NEAR_FRACTION = 0.2f
private const val STATION_DOT_COLOR = 0xE6FFFFFF.toInt()
private const val STATION_DOT_RADIUS_DP = 1.5f
private const val UNIT_RADIUS = 1f
private const val CENTER = 0f
private const val FULL_ALPHA = 255
private const val FULL_OPACITY = 1f

internal class RadarStationBeamDrawer(
    private val density: Float
) {

    var opacity: Float = FULL_OPACITY

    private val stationPositions: List<LatLng> = radarStations.map { radarStation: RadarStation ->
        LatLng(radarStation.latitude, radarStation.longitude)
    }
    private val rangePositions: List<LatLng> = radarStations.map { radarStation: RadarStation ->
        LatLng(radarStation.latitude + RADAR_STATION_RANGE_KILOMETERS / KILOMETERS_PER_DEGREE_OF_LATITUDE, radarStation.longitude)
    }
    private val unitOval: RectF = RectF(-UNIT_RADIUS, -UNIT_RADIUS, UNIT_RADIUS, UNIT_RADIUS)
    private val beamPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = beamShader() }
    private val stationDotPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = STATION_DOT_COLOR }

    fun draw(
        canvas: Canvas,
        projection: Projection,
        timeMilliseconds: Long
    ) {
        beamPaint.alpha = (opacity * FULL_ALPHA).roundToInt()
        stationDotPaint.alpha = (opacity * Color.alpha(STATION_DOT_COLOR)).roundToInt()
        stationPositions.forEachIndexed { stationIndex: Int, stationPosition: LatLng ->
            val station: PointF = projection.toScreenLocation(stationPosition)
            val range: PointF = projection.toScreenLocation(rangePositions[stationIndex])
            val radius: Float = hypot(x = range.x - station.x, y = range.y - station.y)
            val isVisible: Boolean = station.x + radius > 0f &&
                station.x - radius < canvas.width &&
                station.y + radius > 0f &&
                station.y - radius < canvas.height
            if (isVisible) {
                drawBeam(
                    canvas = canvas,
                    station = station,
                    radius = radius,
                    degrees = beamDegrees(
                        stationIndex = stationIndex,
                        timeMilliseconds = timeMilliseconds
                    )
                )
                canvas.drawCircle(station.x, station.y, STATION_DOT_RADIUS_DP * density, stationDotPaint)
            }
        }
    }

    private fun drawBeam(
        canvas: Canvas,
        station: PointF,
        radius: Float,
        degrees: Float
    ) {
        val saveCount: Int = canvas.save()
        canvas.translate(station.x, station.y)
        canvas.rotate(degrees)
        canvas.scale(radius, radius)
        canvas.drawArc(unitOval, -TRAIL_DEGREES, TRAIL_DEGREES + LEADING_EDGE_DEGREES, true, beamPaint)
        canvas.restoreToCount(saveCount)
    }

    private fun beamShader(): Shader {
        val sweep = SweepGradient(
            CENTER,
            CENTER,
            intArrayOf(BEAM_COLOR, NO_COLOR, NO_COLOR, EASED_BEAM_COLOR, BEAM_COLOR),
            floatArrayOf(
                0f,
                LEADING_EDGE_DEGREES / DEGREES_IN_CIRCLE,
                (DEGREES_IN_CIRCLE - TRAIL_DEGREES) / DEGREES_IN_CIRCLE,
                (DEGREES_IN_CIRCLE - TRAIL_DEGREES * TRAIL_EASE_FRACTION) / DEGREES_IN_CIRCLE,
                1f
            )
        )
        val fade = RadialGradient(
            CENTER,
            CENTER,
            UNIT_RADIUS,
            intArrayOf(OPAQUE_MASK, NEAR_MASK, NO_COLOR),
            floatArrayOf(0f, NEAR_FRACTION, 1f),
            Shader.TileMode.CLAMP
        )
        return ComposeShader(sweep, fade, PorterDuff.Mode.DST_IN)
    }
}