package se.vadretapp.vadret.radar.ui.map

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.FrameLayout
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.Lifecycle
import kotlinx.collections.immutable.ImmutableList
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.radar.R
import se.vadretapp.vadret.radar.model.PollenMarker
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.precipitation.PrecipitationOverlay
import se.vadretapp.vadret.radar.ui.map.extension.toCameraPosition
import se.vadretapp.vadret.radar.ui.map.extension.toRadarCamera
import se.vadretapp.vadret.radar.wind.WindAtLocation
import se.vadretapp.vadret.radar.wind.WindField
import java.util.Locale
import kotlin.math.abs

private const val ZOOM_TOLERANCE = 0.01
private const val NO_OBSCURED_HEIGHT = 0
private const val WIND_FADE_IN_MILLISECONDS = 400L
private const val NO_OPACITY = 0f
private const val FULL_OPACITY = 1f
private const val MAXIMUM_ZOOM_LEVEL = 12.0
private const val PRECIPITATION_LAYER_ID = "vadret-precipitation"
private const val PRECIPITATION_OPACITY = 1f
private const val WIND_LAYER_ID = "vadret-wind"
private const val WIND_OPACITY = 0.8f
private const val POLLEN_LAYER_ID = "vadret-pollen"
private const val POLLEN_OPACITY = 1f
private const val BACKGROUND_LAYER_ID = "background"
private const val WATER_LAYER_ID = "water"
private const val DARK_LAND_COLOR = "#181512"
private const val DARK_WATER_COLOR = "#21364B"
private const val DARK_LABEL_COLOR = "#FFFFFF"
private const val NO_PROGRESS = 0f

internal class RadarMapView(
    context: Context,
    private val initialCamera: RadarCamera,
    private val onCameraIdle: (RadarCamera) -> Unit
) : FrameLayout(context) {

    private val density: Float = resources.displayMetrics.density
    private val windParticleDrawer = WindParticleDrawer(density = density)
    private val radarStationBeamDrawer = RadarStationBeamDrawer(density = density)
    private val locationMarkerDrawer = LocationMarkerDrawer(
        density = density,
        scaledDensity = density * resources.configuration.fontScale,
        arrow = mutableArrow(context = context)
    )
    private val pollenMarkerDrawer = PollenMarkerDrawer(density = density)
    private val overlayView = RadarOverlayView(
        context = context,
        windParticleDrawer = windParticleDrawer,
        radarStationBeamDrawer = radarStationBeamDrawer,
        locationMarkerDrawer = locationMarkerDrawer,
        pollenMarkerDrawer = pollenMarkerDrawer
    )
    private val mapView: MapView = createMapView(context = context)
    private var precipitationLayer: RadarImageLayer? = null
    private var windLayer: RadarImageLayer? = null
    private var pollenLayer: RadarImageLayer? = null
    private var pollenClouds: ImageBitmap? = null
    private var layer: RadarLayer = RadarLayer.PRECIPITATION
    private var precipitationOverlay: PrecipitationOverlay? = null
    private var windField: WindField? = null
    private var lifecycleState: Lifecycle.State = Lifecycle.State.CREATED
    private var isMapLoaded: Boolean = false
    private var onMapLoaded: (() -> Unit)? = null
    private var map: MapLibreMap? = null
    private var isDark: Boolean = false
    private var appearance: RadarMapAppearance? = null
    private var limitedZoom: Double = Double.NaN
    private var obscuredBottom: Int = NO_OBSCURED_HEIGHT
    private var windOpacity: Float = FULL_OPACITY
    private var windFadeIn: ValueAnimator? = null
    private var hasWindFadedIn: Boolean = false
    private val imageLayersUpdate = Runnable { showImageLayers() }

    init {
        addView(mapView)
        addView(overlayView)
        mapView.addOnDidFinishLoadingMapListener { markMapLoaded() }
        mapView.addOnDidFailLoadingMapListener { _: String? -> markMapLoaded() }
        mapView.getMapAsync { map: MapLibreMap -> onMapReady(map = map) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post { map?.triggerRepaint() }
    }

    override fun onSizeChanged(
        width: Int,
        height: Int,
        oldWidth: Int,
        oldHeight: Int
    ) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        limitedZoom = Double.NaN
        val readyMap: MapLibreMap = map ?: return
        limitCamera(map = readyMap)
    }

    fun setOnMapLoadedListener(onMapLoaded: (() -> Unit)?) {
        this.onMapLoaded = onMapLoaded
        if (isMapLoaded && onMapLoaded != null) {
            onMapLoaded()
        }
    }

    fun setAppearance(radarMapAppearance: RadarMapAppearance) {
        if (radarMapAppearance == appearance) {
            return
        }
        appearance = radarMapAppearance
        locationMarkerDrawer.setColors(
            markerColor = radarMapAppearance.markerColor,
            surfaceColor = radarMapAppearance.surfaceColor,
            labelTextColor = radarMapAppearance.labelTextColor
        )
        pollenMarkerDrawer.setBorderColor(borderColor = radarMapAppearance.surfaceColor)
        windParticleDrawer.particleColor = radarMapAppearance.particleColor
        if (radarMapAppearance.isDark != isDark) {
            isDark = radarMapAppearance.isDark
            val readyMap: MapLibreMap? = map
            if (readyMap != null) {
                loadStyle(map = readyMap)
            }
        }
        overlayView.invalidate()
    }

    fun show(
        layer: RadarLayer,
        precipitationOverlay: PrecipitationOverlay?,
        windField: WindField?,
        deviceLocation: Location?,
        windAtLocation: WindAtLocation?,
        windSpeedText: String?,
        areRadarStationsShown: Boolean,
        pollenMarkers: ImmutableList<PollenMarker>,
        pollenClouds: ImageBitmap?
    ) {
        this.layer = layer
        if (precipitationOverlay !== this.precipitationOverlay) {
            overlayView.precipitationProgress = NO_PROGRESS
        }
        this.precipitationOverlay = precipitationOverlay
        overlayView.precipitationEchoes = precipitationOverlay as? PrecipitationOverlay.Echoes
        this.windField = windField
        windParticleDrawer.windField = windField
        locationMarkerDrawer.location = deviceLocation
        locationMarkerDrawer.windAtLocation = windAtLocation
        locationMarkerDrawer.windSpeedText = windSpeedText
        overlayView.isWindShown = layer == RadarLayer.WIND
        overlayView.isPollenShown = layer == RadarLayer.POLLEN
        pollenMarkerDrawer.pollenMarkers = pollenMarkers
        this.pollenClouds = pollenClouds
        overlayView.areRadarStationsShown = areRadarStationsShown
        if (layer == RadarLayer.WIND && windField != null && !hasWindFadedIn) {
            hasWindFadedIn = true
            fadeInWind()
        }
        removeCallbacks(imageLayersUpdate)
        post(imageLayersUpdate)
        overlayView.invalidate()
    }

    fun setObscuredBottom(obscuredBottom: Int) {
        if (obscuredBottom == this.obscuredBottom) {
            return
        }
        this.obscuredBottom = obscuredBottom
        limitedZoom = Double.NaN
        val readyMap: MapLibreMap = map ?: return
        limitCamera(map = readyMap)
    }

    fun setPrecipitationProgress(progress: Float) {
        overlayView.precipitationProgress = progress
        overlayView.invalidate()
    }

    fun advanceWind() {
        overlayView.advanceWind()
    }

    fun moveTo(state: Lifecycle.State) {
        if (lifecycleState < Lifecycle.State.STARTED && state >= Lifecycle.State.STARTED) {
            mapView.onStart()
            lifecycleState = Lifecycle.State.STARTED
        }
        if (lifecycleState < Lifecycle.State.RESUMED && state >= Lifecycle.State.RESUMED) {
            mapView.onResume()
            lifecycleState = Lifecycle.State.RESUMED
        }
        if (lifecycleState == Lifecycle.State.RESUMED && state < Lifecycle.State.RESUMED) {
            mapView.onPause()
            lifecycleState = Lifecycle.State.STARTED
        }
        if (lifecycleState == Lifecycle.State.STARTED && state < Lifecycle.State.STARTED) {
            mapView.onStop()
            lifecycleState = Lifecycle.State.CREATED
        }
    }

    fun destroy() {
        windFadeIn?.cancel()
        removeCallbacks(imageLayersUpdate)
        moveTo(state = Lifecycle.State.CREATED)
        mapView.onDestroy()
    }

    private fun fadeInWind() {
        val animator: ValueAnimator = ValueAnimator.ofFloat(NO_OPACITY, FULL_OPACITY)
        animator.duration = WIND_FADE_IN_MILLISECONDS
        animator.addUpdateListener { updatedAnimator: ValueAnimator ->
            showWindOpacity(opacity = updatedAnimator.animatedValue as Float)
        }
        showWindOpacity(opacity = NO_OPACITY)
        windFadeIn = animator
        animator.start()
    }

    private fun showWindOpacity(opacity: Float) {
        windOpacity = opacity
        windLayer?.setOpacityFraction(opacityFraction = opacity)
        windParticleDrawer.opacity = opacity
        radarStationBeamDrawer.opacity = opacity
        overlayView.invalidate()
    }

    private fun markMapLoaded() {
        isMapLoaded = true
        onMapLoaded?.invoke()
    }

    private fun onMapReady(map: MapLibreMap) {
        configure(map = map)
        this.map = map
        overlayView.map = map
        map.addOnCameraMoveListener {
            limitCamera(map = map)
            overlayView.invalidate()
        }
        map.addOnCameraIdleListener { reportCameraIdle(map = map) }
        loadStyle(map = map)
    }

    private fun loadStyle(map: MapLibreMap) {
        precipitationLayer = null
        windLayer = null
        pollenLayer = null
        val styleUri: String = if (isDark) {
            "https://tiles.openfreemap.org/styles/dark"
        } else {
            "https://tiles.openfreemap.org/styles/positron"
        }
        map.setStyle(Style.Builder().fromUri(styleUri)) { style: Style ->
            style.localizeLabels(language = Locale.getDefault().language)
            if (isDark) {
                style.useDarkMapColors()
            }
            val labelLayerId: String? = style.labelLayerAboveBaseMap()
            precipitationLayer = RadarImageLayer(
                style = style,
                layerId = PRECIPITATION_LAYER_ID,
                opacity = PRECIPITATION_OPACITY,
                belowLayerId = labelLayerId
            )
            windLayer = RadarImageLayer(
                style = style,
                layerId = WIND_LAYER_ID,
                opacity = WIND_OPACITY,
                belowLayerId = labelLayerId
            )
            pollenLayer = RadarImageLayer(
                style = style,
                layerId = POLLEN_LAYER_ID,
                opacity = POLLEN_OPACITY,
                belowLayerId = labelLayerId
            )
            windLayer?.setOpacityFraction(opacityFraction = windOpacity)
            showImageLayers()
            overlayView.invalidate()
        }
    }

    private fun showImageLayers() {
        val coloredPrecipitation: Bitmap? = (precipitationOverlay as? PrecipitationOverlay.Colored)?.image?.asAndroidBitmap()
        precipitationLayer?.show(
            bitmap = coloredPrecipitation,
            isVisible = layer == RadarLayer.PRECIPITATION && coloredPrecipitation != null
        )
        windLayer?.show(
            bitmap = windField?.image?.asAndroidBitmap(),
            isVisible = layer == RadarLayer.WIND
        )
        val pollenBitmap: Bitmap? = pollenClouds?.asAndroidBitmap()
        pollenLayer?.show(
            bitmap = pollenBitmap,
            isVisible = layer == RadarLayer.POLLEN && pollenBitmap != null
        )
    }

    private fun configure(map: MapLibreMap) {
        map.uiSettings.isRotateGesturesEnabled = false
        map.uiSettings.isTiltGesturesEnabled = false
        map.uiSettings.isCompassEnabled = false
        map.uiSettings.isLogoEnabled = false
        map.uiSettings.isAttributionEnabled = false
        map.setMaxZoomPreference(MAXIMUM_ZOOM_LEVEL)
        map.cameraPosition = initialCamera.toCameraPosition()
        limitCamera(map = map)
    }

    private fun limitCamera(map: MapLibreMap) {
        if (width == 0 || height == 0) {
            return
        }
        val zoom: Double = map.cameraPosition.zoom
        if (abs(x = zoom - limitedZoom) < ZOOM_TOLERANCE) {
            return
        }
        limitedZoom = zoom
        val radarCameraLimits: RadarCameraLimits = radarCameraLimits(
            viewportWidth = width / density.toDouble(),
            viewportHeight = height / density.toDouble(),
            obscuredBottom = obscuredBottom / density.toDouble(),
            zoom = zoom
        )
        map.setMinZoomPreference(radarCameraLimits.minimumZoom)
        map.setLatLngBoundsForCameraTarget(
            LatLngBounds.from(
                radarCameraLimits.northLatitude,
                radarCameraLimits.eastLongitude,
                radarCameraLimits.southLatitude,
                radarCameraLimits.westLongitude
            )
        )
    }

    private fun reportCameraIdle(map: MapLibreMap) {
        val radarCamera: RadarCamera = map.cameraPosition.toRadarCamera() ?: return
        onCameraIdle(radarCamera)
    }
}

private fun Style.labelLayerAboveBaseMap(): String? {
    val styleLayers: List<Layer> = layers
    val lastBaseMapIndex: Int = styleLayers.indexOfLast { styleLayer: Layer -> styleLayer !is SymbolLayer }
    return styleLayers.getOrNull(index = lastBaseMapIndex + 1)?.id
}

private fun Style.useDarkMapColors() {
    getLayer(BACKGROUND_LAYER_ID)?.setProperties(PropertyFactory.backgroundColor(DARK_LAND_COLOR))
    getLayer(WATER_LAYER_ID)?.setProperties(PropertyFactory.fillColor(DARK_WATER_COLOR))
    layers
        .filterIsInstance<SymbolLayer>()
        .forEach { symbolLayer: SymbolLayer -> symbolLayer.setProperties(PropertyFactory.textColor(DARK_LABEL_COLOR)) }
}

private fun createMapView(context: Context): MapView {
    MapLibre.getInstance(context)
    val mapView = MapView(context)
    mapView.onCreate(null)
    return mapView
}

private fun mutableArrow(context: Context): Drawable = requireNotNull(context.getDrawable(R.drawable.ic_north)).mutate()