package se.vadretapp.vadret.radar.ui.map

import androidx.compose.runtime.Composable
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.Anchor
import org.maplibre.compose.layers.LayerSummary
import org.maplibre.compose.layers.RasterLayer
import org.maplibre.compose.sources.rememberImageSource
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.compose.util.PositionQuad
import org.maplibre.compose.util.PreparedImage
import org.maplibre.spatialk.geojson.Position
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.projection.RADAR_EAST_LONGITUDE
import se.vadretapp.vadret.radar.projection.RADAR_NORTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_SOUTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_WEST_LONGITUDE

private const val PRECIPITATION_LAYER_ID = "vadret-precipitation"
private const val WIND_LAYER_ID = "vadret-wind"
private const val PRECIPITATION_OPACITY = 1f
private const val WIND_OPACITY = 0.8f
private const val POLLEN_LAYER_ID = "vadret-pollen"
private const val POLLEN_OPACITY = 1f
private const val SYMBOL_LAYER_TYPE = "symbol"

private val radarArea = PositionQuad(
    topLeft = Position(longitude = RADAR_WEST_LONGITUDE, latitude = RADAR_NORTH_LATITUDE),
    topRight = Position(longitude = RADAR_EAST_LONGITUDE, latitude = RADAR_NORTH_LATITUDE),
    bottomRight = Position(longitude = RADAR_EAST_LONGITUDE, latitude = RADAR_SOUTH_LATITUDE),
    bottomLeft = Position(longitude = RADAR_WEST_LONGITUDE, latitude = RADAR_SOUTH_LATITUDE)
)

internal data class RadarMapLayers(
    val layer: RadarLayer,
    val coloredPrecipitation: PreparedImage?,
    val windImage: PreparedImage?,
    val windOpacity: Float,
    val pollenImage: PreparedImage?
)

@Composable
@MaplibreComposable
internal fun RadarRasterLayers(radarMapLayers: RadarMapLayers) {
    val coloredPrecipitation: PreparedImage? = radarMapLayers.coloredPrecipitation
    val windImage: PreparedImage? = radarMapLayers.windImage
    val pollenImage: PreparedImage? = radarMapLayers.pollenImage
    Anchor.Above(predicate = { layerSummary: LayerSummary -> layerSummary.type != SYMBOL_LAYER_TYPE }) {
        if (coloredPrecipitation != null) {
            RasterLayer(
                id = PRECIPITATION_LAYER_ID,
                source = rememberImageSource(
                    position = radarArea,
                    image = coloredPrecipitation
                ),
                visible = radarMapLayers.layer == RadarLayer.PRECIPITATION,
                opacity = const(PRECIPITATION_OPACITY)
            )
        }
        if (windImage != null) {
            RasterLayer(
                id = WIND_LAYER_ID,
                source = rememberImageSource(
                    position = radarArea,
                    image = windImage
                ),
                visible = radarMapLayers.layer == RadarLayer.WIND,
                opacity = const(WIND_OPACITY * radarMapLayers.windOpacity)
            )
        }
        if (pollenImage != null) {
            RasterLayer(
                id = POLLEN_LAYER_ID,
                source = rememberImageSource(
                    position = radarArea,
                    image = pollenImage
                ),
                visible = radarMapLayers.layer == RadarLayer.POLLEN,
                opacity = const(POLLEN_OPACITY)
            )
        }
    }
}