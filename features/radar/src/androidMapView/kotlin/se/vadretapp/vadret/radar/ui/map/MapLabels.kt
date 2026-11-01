package se.vadretapp.vadret.radar.ui.map

import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer

private const val NAME_PROPERTY = "name"
private const val LATIN_NAME_PROPERTY = "name:latin"
private const val NAME_REFERENCE = "\"name"

internal fun Style.localizeLabels(language: String) {
    val localizedName: Expression = Expression.coalesce(
        Expression.get("$NAME_PROPERTY:$language"),
        Expression.get(LATIN_NAME_PROPERTY),
        Expression.get(NAME_PROPERTY)
    )
    layers.forEach { layer: Layer ->
        if (layer is SymbolLayer && layer.showsPlaceName()) {
            layer.setProperties(PropertyFactory.textField(localizedName))
        }
    }
}

private fun SymbolLayer.showsPlaceName(): Boolean = textField.expression?.toString()?.contains(NAME_REFERENCE) == true