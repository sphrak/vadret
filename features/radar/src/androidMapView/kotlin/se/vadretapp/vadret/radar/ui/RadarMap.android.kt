package se.vadretapp.vadret.radar.ui

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.precipitation.PrecipitationMotion
import se.vadretapp.vadret.radar.precipitation.PrecipitationOverlay
import se.vadretapp.vadret.radar.ui.map.RadarMapAppearance
import se.vadretapp.vadret.radar.ui.map.RadarMapStore
import se.vadretapp.vadret.radar.ui.map.RadarMapView
import se.vadretapp.vadret.radar.wind.WindAtLocation
import se.vadretapp.vadret.radar.wind.WindField
import se.vadretapp.vadret.ui.VadretTheme

private const val PARTICLE_FRAME_NANOS = 33_000_000L
private const val DAY_PARTICLE_COLOR = 0xD9FFFFFF.toInt()
private const val NIGHT_PARTICLE_COLOR = 0xB3D2DCFF.toInt()
private const val NO_PROGRESS = 0f
private const val FULL_PROGRESS = 1f

@Composable
internal actual fun RadarMap(
    layer: RadarLayer,
    precipitationOverlay: PrecipitationOverlay?,
    windField: WindField?,
    deviceLocation: Location?,
    windAtLocation: WindAtLocation?,
    windSpeedText: String?,
    areRadarStationsShown: Boolean,
    obscuredBottom: Int,
    onMapLoaded: () -> Unit,
    modifier: Modifier
) {
    val radarMapStore: RadarMapStore = LocalRadarMapStore.current
    val isDark: Boolean = VadretTheme.isDark
    val radarMapAppearance = RadarMapAppearance(
        isDark = isDark,
        markerColor = VadretTheme.colors.accent.toArgb(),
        surfaceColor = VadretTheme.colorScheme.surface.toArgb(),
        labelTextColor = VadretTheme.colorScheme.onSurface.toArgb(),
        particleColor = if (isDark) {
            NIGHT_PARTICLE_COLOR
        } else {
            DAY_PARTICLE_COLOR
        }
    )
    val currentOnMapLoaded: () -> Unit by rememberUpdatedState(newValue = onMapLoaded)
    val radarMapView: RadarMapView = remember(radarMapStore) { radarMapStore.radarMapView() }
    MapShownEffect(
        radarMapStore = radarMapStore,
        radarMapView = radarMapView,
        onMapLoaded = { currentOnMapLoaded() }
    )
    WindAnimationEffect(
        isAnimating = layer == RadarLayer.WIND && windField != null,
        radarMapView = radarMapView
    )
    PrecipitationMotionEffect(
        precipitationMotion = (precipitationOverlay as? PrecipitationOverlay.Echoes)?.motion,
        radarMapView = radarMapView
    )

    AndroidView(
        factory = { radarMapView },
        update = { view: RadarMapView ->
            view.setObscuredBottom(obscuredBottom = obscuredBottom)
            view.setAppearance(radarMapAppearance = radarMapAppearance)
            view.show(
                layer = layer,
                precipitationOverlay = precipitationOverlay,
                windField = windField,
                deviceLocation = deviceLocation,
                windAtLocation = windAtLocation,
                windSpeedText = windSpeedText,
                areRadarStationsShown = areRadarStationsShown
            )
        },
        onRelease = { view: RadarMapView ->
            val parent: ViewGroup? = view.parent as? ViewGroup
            parent?.removeView(view)
        },
        modifier = modifier
    )
}

@Composable
private fun MapShownEffect(
    radarMapStore: RadarMapStore,
    radarMapView: RadarMapView,
    onMapLoaded: () -> Unit
) {
    DisposableEffect(radarMapStore, radarMapView) {
        radarMapView.setOnMapLoadedListener(onMapLoaded = onMapLoaded)
        radarMapStore.onShownChanged(isShown = true)
        onDispose {
            radarMapView.setOnMapLoadedListener(onMapLoaded = null)
            radarMapStore.onShownChanged(isShown = false)
        }
    }
}

@Composable
private fun WindAnimationEffect(
    isAnimating: Boolean,
    radarMapView: RadarMapView
) {
    LaunchedEffect(isAnimating, radarMapView) {
        var lastFrameNanos = 0L
        while (isAnimating) {
            withFrameNanos { frameNanos: Long ->
                if (frameNanos - lastFrameNanos >= PARTICLE_FRAME_NANOS) {
                    lastFrameNanos = frameNanos
                    radarMapView.advanceWind()
                }
            }
        }
    }
}

@Composable
private fun PrecipitationMotionEffect(
    precipitationMotion: PrecipitationMotion?,
    radarMapView: RadarMapView
) {
    LaunchedEffect(precipitationMotion, radarMapView) {
        if (precipitationMotion == null) {
            return@LaunchedEffect
        }
        val durationNanos: Float = precipitationMotion.duration.inWholeNanoseconds.toFloat()
        val firstFrameNanos: Long = withFrameNanos { frameNanos: Long -> frameNanos }
        var frameNanos: Long = withFrameNanos { secondFrameNanos: Long -> secondFrameNanos }
        val shownNanos: Long = firstFrameNanos - (frameNanos - firstFrameNanos)
        var progress: Float = NO_PROGRESS
        while (progress < FULL_PROGRESS) {
            progress = ((frameNanos - shownNanos) / durationNanos).coerceAtMost(maximumValue = FULL_PROGRESS)
            radarMapView.setPrecipitationProgress(progress = progress)
            frameNanos = withFrameNanos { nextFrameNanos: Long -> nextFrameNanos }
        }
    }
}