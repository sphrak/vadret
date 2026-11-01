package se.vadretapp.vadret.radar.ui.map

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.maplibre.compose.layers.LayerHandle
import org.maplibre.compose.layers.MutableLayerHandle
import org.maplibre.compose.map.MapStyleState

private const val SYMBOL_LAYER_TYPE = "symbol"
private const val TEXT_FIELD_PROPERTY = "text-field"
private const val TEXT_COLOR_PROPERTY = "text-color"
private const val BACKGROUND_COLOR_PROPERTY = "background-color"
private const val FILL_COLOR_PROPERTY = "fill-color"
private const val BACKGROUND_LAYER_ID = "background"
private const val WATER_LAYER_ID = "water"
private const val DARK_LAND_COLOR = "#181512"
private const val DARK_WATER_COLOR = "#21364B"
private const val DARK_LABEL_COLOR = "#FFFFFF"
private const val NAME_PROPERTY = "name"
private const val LATIN_NAME_PROPERTY = "name:latin"
private const val NAME_REFERENCE = "\"name"
private const val COALESCE_OPERATOR = "coalesce"
private const val GET_OPERATOR = "get"

internal suspend fun MapStyleState.restyle(
    language: String,
    isDark: Boolean
) {
    val localizedName: JsonElement = JsonArray(
        content = listOf(
            JsonPrimitive(value = COALESCE_OPERATOR),
            property(name = "$NAME_PROPERTY:$language"),
            property(name = LATIN_NAME_PROPERTY),
            property(name = NAME_PROPERTY)
        )
    )
    layers
        .filter { layerHandle: LayerHandle -> layerHandle.type == SYMBOL_LAYER_TYPE }
        .forEach { layerHandle: LayerHandle ->
            val mutableLayerHandle: MutableLayerHandle? = layerHandle.asMutable
            if (mutableLayerHandle != null) {
                if (layerHandle.showsPlaceName()) {
                    mutableLayerHandle.setLayoutProperty(
                        name = TEXT_FIELD_PROPERTY,
                        value = localizedName
                    )
                }
                if (isDark) {
                    mutableLayerHandle.setPaintProperty(
                        name = TEXT_COLOR_PROPERTY,
                        value = JsonPrimitive(value = DARK_LABEL_COLOR)
                    )
                }
            }
        }
    if (isDark) {
        layers[BACKGROUND_LAYER_ID]?.asMutable?.setPaintProperty(
            name = BACKGROUND_COLOR_PROPERTY,
            value = JsonPrimitive(value = DARK_LAND_COLOR)
        )
        layers[WATER_LAYER_ID]?.asMutable?.setPaintProperty(
            name = FILL_COLOR_PROPERTY,
            value = JsonPrimitive(value = DARK_WATER_COLOR)
        )
    }
}

private suspend fun LayerHandle.showsPlaceName(): Boolean = getProperty(name = TEXT_FIELD_PROPERTY)?.toString()?.contains(NAME_REFERENCE) == true

private fun property(name: String): JsonElement =
    JsonArray(
        content = listOf(
            JsonPrimitive(value = GET_OPERATOR),
            JsonPrimitive(value = name)
        )
    )