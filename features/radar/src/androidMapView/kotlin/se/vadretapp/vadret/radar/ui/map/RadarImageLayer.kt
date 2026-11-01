package se.vadretapp.vadret.radar.ui.map

import android.graphics.Bitmap
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngQuad
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.ImageSource
import se.vadretapp.vadret.radar.projection.RADAR_EAST_LONGITUDE
import se.vadretapp.vadret.radar.projection.RADAR_NORTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_SOUTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_WEST_LONGITUDE

private const val NO_FADE_MILLISECONDS = 0f
private const val FULL_OPACITY_FRACTION = 1f

private val radarArea = LatLngQuad(
    LatLng(RADAR_NORTH_LATITUDE, RADAR_WEST_LONGITUDE),
    LatLng(RADAR_NORTH_LATITUDE, RADAR_EAST_LONGITUDE),
    LatLng(RADAR_SOUTH_LATITUDE, RADAR_EAST_LONGITUDE),
    LatLng(RADAR_SOUTH_LATITUDE, RADAR_WEST_LONGITUDE)
)

internal class RadarImageLayer(
    private val style: Style,
    private val layerId: String,
    private val opacity: Float,
    private val belowLayerId: String?
) {

    private var shownBitmap: Bitmap? = null
    private var shownVisibility: String? = null
    private var opacityFraction: Float = FULL_OPACITY_FRACTION

    fun setOpacityFraction(opacityFraction: Float) {
        this.opacityFraction = opacityFraction
        style.getLayer(layerId)?.setProperties(PropertyFactory.rasterOpacity(opacity * opacityFraction))
    }

    fun show(
        bitmap: Bitmap?,
        isVisible: Boolean
    ) {
        if (bitmap != null && bitmap !== shownBitmap) {
            val imageSource: ImageSource? = style.getSourceAs(layerId)
            if (imageSource == null) {
                add(bitmap = bitmap)
            } else {
                imageSource.setImage(bitmap)
            }
            shownBitmap = bitmap
        }
        val visibility: String = if (isVisible && shownBitmap != null) {
            Property.VISIBLE
        } else {
            Property.NONE
        }
        if (visibility == shownVisibility) {
            return
        }
        shownVisibility = visibility
        style.getLayer(layerId)?.setProperties(PropertyFactory.visibility(visibility))
    }

    private fun add(bitmap: Bitmap) {
        style.addSource(ImageSource(layerId, radarArea, bitmap))
        val rasterLayer: RasterLayer = RasterLayer(layerId, layerId).withProperties(
            PropertyFactory.rasterOpacity(opacity * opacityFraction),
            PropertyFactory.rasterFadeDuration(NO_FADE_MILLISECONDS)
        )
        if (belowLayerId != null) {
            style.addLayerBelow(rasterLayer, belowLayerId)
        } else {
            style.addLayer(rasterLayer)
        }
    }
}