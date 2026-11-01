package se.vadretapp.vadret.radar.ui.map

import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.view.View
import org.maplibre.android.maps.MapLibreMap
import se.vadretapp.vadret.radar.precipitation.PrecipitationOverlay

private const val NO_PROGRESS = 0f

internal class RadarOverlayView(
    context: Context,
    private val windParticleDrawer: WindParticleDrawer,
    private val radarStationBeamDrawer: RadarStationBeamDrawer,
    private val locationMarkerDrawer: LocationMarkerDrawer
) : View(context) {

    var map: MapLibreMap? = null
    var isWindShown: Boolean = false
    var areRadarStationsShown: Boolean = false
    var precipitationEchoes: PrecipitationOverlay.Echoes? = null
    var precipitationProgress: Float = NO_PROGRESS

    private val precipitationShaderDrawer: PrecipitationShaderDrawer? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PrecipitationShaderDrawer()
    } else {
        null
    }

    fun advanceWind() {
        val currentMap: MapLibreMap = map ?: return
        windParticleDrawer.advance(
            projection = currentMap.projection,
            screenWidth = width,
            screenHeight = height
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val currentMap: MapLibreMap = map ?: return
        if (isWindShown && areRadarStationsShown) {
            radarStationBeamDrawer.draw(
                canvas = canvas,
                projection = currentMap.projection,
                timeMilliseconds = drawingTime
            )
        }
        if (isWindShown) {
            windParticleDrawer.draw(
                canvas = canvas,
                projection = currentMap.projection
            )
        } else {
            drawPrecipitation(
                canvas = canvas,
                currentMap = currentMap
            )
        }
        locationMarkerDrawer.draw(
            canvas = canvas,
            projection = currentMap.projection
        )
    }

    private fun drawPrecipitation(
        canvas: Canvas,
        currentMap: MapLibreMap
    ) {
        val echoesOverlay: PrecipitationOverlay.Echoes = precipitationEchoes ?: return
        val shaderDrawer: PrecipitationShaderDrawer = precipitationShaderDrawer ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shaderDrawer.draw(
                canvas = canvas,
                projection = currentMap.projection,
                echoesOverlay = echoesOverlay,
                progress = precipitationProgress
            )
        }
    }
}