package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.radar.model.PollenMarker
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.precipitation.PrecipitationMotion
import se.vadretapp.vadret.radar.precipitation.PrecipitationOverlay
import se.vadretapp.vadret.radar.ui.map.RasterProjection
import se.vadretapp.vadret.radar.wind.WindAtLocation
import se.vadretapp.vadret.radar.wind.WindField
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ic_north

private const val PARTICLE_FRAME_NANOS = 33_000_000L
private const val WIND_FADE_IN_MILLISECONDS = 400
private const val DAY_PARTICLE_COLOR = 0xD9FFFFFF
private const val NIGHT_PARTICLE_COLOR = 0xB3D2DCFF
private const val NO_PROGRESS = 0f
private const val FULL_PROGRESS = 1f
private const val NO_OPACITY = 0f
private const val FULL_OPACITY = 1f
private const val NO_TIME = 0L
private const val NO_SIZE = 0f
private const val LABEL_TEXT_SP = 14
private const val TABULAR_NUMBERS = "tnum"

@Composable
internal fun RadarOverlay(
    screenProjection: () -> ScreenProjection,
    layer: RadarLayer,
    precipitationOverlay: PrecipitationOverlay?,
    windField: WindField?,
    windOpacity: Float,
    deviceLocation: Location?,
    windAtLocation: WindAtLocation?,
    windSpeedText: String?,
    areRadarStationsShown: Boolean,
    pollenMarkers: ImmutableList<PollenMarker>,
    modifier: Modifier = Modifier
) {
    val isWindShown: Boolean = layer == RadarLayer.WIND
    val isDark: Boolean = VadretTheme.isDark
    val particleColor: Int = Color(
        color = if (isDark) {
            NIGHT_PARTICLE_COLOR
        } else {
            DAY_PARTICLE_COLOR
        }
    ).toArgb()
    val markerColor: Color = VadretTheme.colors.accent
    val surfaceColor: Color = VadretTheme.colorScheme.surface
    val labelStyle = TextStyle(
        color = VadretTheme.colorScheme.onSurface,
        fontSize = LABEL_TEXT_SP.sp,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = TABULAR_NUMBERS
    )
    val arrow: Painter = painterResource(resource = Res.drawable.ic_north)
    val textMeasurer: TextMeasurer = rememberTextMeasurer()
    val density: Float = LocalDensity.current.density
    val windParticleLayer: WindParticleLayer = remember { WindParticleLayer() }
    val stationBeamLayer: StationBeamLayer = remember { StationBeamLayer() }
    val precipitationLayer: PrecipitationLayer? = remember {
        val precipitationShader: PrecipitationShader? = precipitationShaderOrNull()
        if (precipitationShader != null) {
            PrecipitationLayer(precipitationShader = precipitationShader)
        } else {
            null
        }
    }
    val locationMarkerLayer: LocationMarkerLayer = remember(markerColor, surfaceColor) {
        LocationMarkerLayer(
            markerColor = markerColor,
            surfaceColor = surfaceColor
        )
    }
    val pollenMarkerLayer: PollenMarkerLayer = remember(surfaceColor) { PollenMarkerLayer(borderColor = surfaceColor) }
    val canvasSize: CanvasSize = remember { CanvasSize() }
    ParticleAnimationEffect(
        isAnimating = isWindShown && windField != null,
        onFrame = {
            val shownWindField: WindField? = windField
            val rasterProjection: RasterProjection? = shownWindField?.rasterProjectionOn(screenProjection = screenProjection())
            if (shownWindField != null && rasterProjection != null) {
                windParticleLayer.advance(
                    windField = shownWindField,
                    rasterProjection = rasterProjection,
                    screenWidth = canvasSize.width,
                    screenHeight = canvasSize.height,
                    density = density
                )
            }
        }
    )
    val beamTime: Long = rememberFrameTime(isRunning = isWindShown && areRadarStationsShown)
    val precipitationProgress: Float = rememberPrecipitationProgress(
        precipitationMotion = (precipitationOverlay as? PrecipitationOverlay.Echoes)?.motion
    )

    Canvas(modifier = modifier) {
        val currentScreenProjection: ScreenProjection = screenProjection()
        canvasSize.width = size.width
        canvasSize.height = size.height
        if (isWindShown && areRadarStationsShown) {
            with(receiver = stationBeamLayer) {
                drawBeams(
                    screenProjection = currentScreenProjection,
                    timeMilliseconds = beamTime,
                    opacity = windOpacity
                )
            }
        }
        if (isWindShown) {
            val shownWindField: WindField? = windField
            val rasterProjection: RasterProjection? = shownWindField?.rasterProjectionOn(screenProjection = currentScreenProjection)
            if (rasterProjection != null) {
                with(receiver = windParticleLayer) {
                    drawParticles(
                        rasterProjection = rasterProjection,
                        particleColor = particleColor,
                        opacity = windOpacity
                    )
                }
            }
        } else if (layer == RadarLayer.POLLEN) {
            with(receiver = pollenMarkerLayer) {
                drawPollenMarkers(
                    screenProjection = currentScreenProjection,
                    pollenMarkers = pollenMarkers
                )
            }
        } else {
            val echoesOverlay: PrecipitationOverlay.Echoes? = precipitationOverlay as? PrecipitationOverlay.Echoes
            if (echoesOverlay != null && precipitationLayer != null) {
                with(receiver = precipitationLayer) {
                    drawPrecipitation(
                        screenProjection = currentScreenProjection,
                        echoesOverlay = echoesOverlay,
                        progress = precipitationProgress
                    )
                }
            }
        }
        if (deviceLocation != null) {
            with(receiver = locationMarkerLayer) {
                drawMarker(
                    screenProjection = currentScreenProjection,
                    location = deviceLocation,
                    windAtLocation = windAtLocation,
                    windSpeedText = windSpeedText,
                    arrow = arrow,
                    textMeasurer = textMeasurer,
                    labelStyle = labelStyle
                )
            }
        }
    }
}

@Composable
internal fun rememberWindOpacity(isWindShown: Boolean): Float {
    val windOpacity: Animatable<Float, *> = remember { Animatable(initialValue = NO_OPACITY) }
    LaunchedEffect(isWindShown) {
        if (isWindShown && windOpacity.value < FULL_OPACITY) {
            windOpacity.animateTo(
                targetValue = FULL_OPACITY,
                animationSpec = tween(durationMillis = WIND_FADE_IN_MILLISECONDS)
            )
        }
    }
    return windOpacity.value
}

private fun WindField.rasterProjectionOn(screenProjection: ScreenProjection): RasterProjection? =
    screenProjection.toRasterProjection(
        rasterWidth = values.width,
        rasterHeight = values.height
    )

private class CanvasSize {
    var width: Float = NO_SIZE
    var height: Float = NO_SIZE
}

@Composable
private fun ParticleAnimationEffect(
    isAnimating: Boolean,
    onFrame: () -> Unit
) {
    val currentOnFrame: () -> Unit by rememberUpdatedState(newValue = onFrame)
    LaunchedEffect(isAnimating) {
        var lastFrameNanos: Long = NO_TIME
        while (isAnimating) {
            withFrameNanos { frameNanos: Long ->
                if (frameNanos - lastFrameNanos >= PARTICLE_FRAME_NANOS) {
                    lastFrameNanos = frameNanos
                    currentOnFrame()
                }
            }
        }
    }
}

@Composable
private fun rememberFrameTime(isRunning: Boolean): Long {
    var frameTime: Long by remember { mutableLongStateOf(value = NO_TIME) }
    LaunchedEffect(isRunning) {
        while (isRunning) {
            withFrameMillis { frameMillis: Long -> frameTime = frameMillis }
        }
    }
    return frameTime
}

@Composable
private fun rememberPrecipitationProgress(precipitationMotion: PrecipitationMotion?): Float {
    var progress: Float by remember(precipitationMotion) { mutableFloatStateOf(value = NO_PROGRESS) }
    LaunchedEffect(precipitationMotion) {
        if (precipitationMotion == null) {
            return@LaunchedEffect
        }
        val durationNanos: Float = precipitationMotion.duration.inWholeNanoseconds.toFloat()
        val firstFrameNanos: Long = withFrameNanos { frameNanos: Long -> frameNanos }
        var frameNanos: Long = withFrameNanos { secondFrameNanos: Long -> secondFrameNanos }
        val shownNanos: Long = firstFrameNanos - (frameNanos - firstFrameNanos)
        var currentProgress: Float = NO_PROGRESS
        while (currentProgress < FULL_PROGRESS) {
            currentProgress = ((frameNanos - shownNanos) / durationNanos).coerceAtMost(maximumValue = FULL_PROGRESS)
            progress = currentProgress
            frameNanos = withFrameNanos { nextFrameNanos: Long -> nextFrameNanos }
        }
    }
    return progress
}