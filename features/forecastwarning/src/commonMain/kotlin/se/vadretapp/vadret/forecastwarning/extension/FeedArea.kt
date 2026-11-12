package se.vadretapp.vadret.forecastwarning.extension

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import se.vadretapp.vadret.aggregatedfeed.model.FeedArea
import se.vadretapp.vadret.aggregatedfeed.model.GeoLine
import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.GeoPosition
import se.vadretapp.vadret.forecastwarning.model.MapBounds
import se.vadretapp.vadret.forecastwarning.model.WarningMapArea
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val TYPE = "type"
private const val GEOMETRY = "geometry"
private const val COORDINATES = "coordinates"
private const val FEATURES = "features"
private const val PROPERTIES = "properties"
private const val FEATURE_COLLECTION = "FeatureCollection"
private const val FEATURE = "Feature"
private const val POLYGON = "Polygon"
private const val LINE_STRING = "LineString"
private const val POINT = "Point"
private const val MINIMUM_LINE_SIZE = 2
private const val CIRCLE_POINT_COUNT = 64
private const val HALF_TURN_DEGREES = 180.0
private const val FULL_TURN_RADIANS = 2 * PI

private val swedenBounds = MapBounds(
    west = 10.9,
    south = 55.3,
    east = 24.2,
    north = 69.1
)

internal fun FeedArea.toEnclosingCircle(): FeedArea {
    val positions: List<GeoPosition> = polygons.flatMap { geoPolygon: GeoPolygon -> geoPolygon.outline }
    if (positions.isEmpty()) {
        return this
    }
    val south: Double = positions.minOf { geoPosition: GeoPosition -> geoPosition.latitude }
    val north: Double = positions.maxOf { geoPosition: GeoPosition -> geoPosition.latitude }
    val west: Double = positions.minOf { geoPosition: GeoPosition -> geoPosition.longitude }
    val east: Double = positions.maxOf { geoPosition: GeoPosition -> geoPosition.longitude }
    val center = GeoPosition(
        latitude = (south + north) / 2,
        longitude = (west + east) / 2
    )
    val longitudeScale: Double = cos(x = center.latitude * PI / HALF_TURN_DEGREES)
    val latitudeRadius: Double = hypot(
        x = (north - south) / 2,
        y = (east - west) / 2 * longitudeScale
    )
    return copy(
        polygons = listOf(
            GeoPolygon(
                outline = (0..CIRCLE_POINT_COUNT).map { pointIndex: Int ->
                    val angle: Double = FULL_TURN_RADIANS * (pointIndex % CIRCLE_POINT_COUNT) / CIRCLE_POINT_COUNT
                    GeoPosition(
                        latitude = center.latitude + latitudeRadius * sin(x = angle),
                        longitude = center.longitude + latitudeRadius / longitudeScale * cos(x = angle)
                    )
                },
                holes = emptyList()
            )
        )
    )
}

internal fun FeedArea.toWarningMapArea(): WarningMapArea {
    val positions: List<GeoPosition> = polygons.flatMap { geoPolygon: GeoPolygon -> geoPolygon.outline } +
        lines.flatMap { geoLine: GeoLine -> geoLine.positions }
    return WarningMapArea(
        geoJson = toGeoJson(),
        bounds = if (positions.isEmpty()) {
            swedenBounds
        } else {
            MapBounds(
                west = positions.minOf { geoPosition: GeoPosition -> geoPosition.longitude },
                south = positions.minOf { geoPosition: GeoPosition -> geoPosition.latitude },
                east = positions.maxOf { geoPosition: GeoPosition -> geoPosition.longitude },
                north = positions.maxOf { geoPosition: GeoPosition -> geoPosition.latitude }
            )
        }
    )
}

private fun FeedArea.toGeoJson(): String? {
    val features: List<JsonObject> = polygons.map { geoPolygon: GeoPolygon ->
        feature(
            type = POLYGON,
            coordinates = JsonArray(
                content = (listOf(geoPolygon.outline) + geoPolygon.holes).map { ring: List<GeoPosition> -> ring.toCoordinates() }
            )
        )
    } + lines.map { geoLine: GeoLine ->
        if (geoLine.positions.size >= MINIMUM_LINE_SIZE) {
            feature(
                type = LINE_STRING,
                coordinates = geoLine.positions.toCoordinates()
            )
        } else {
            feature(
                type = POINT,
                coordinates = geoLine.positions.single().toCoordinate()
            )
        }
    }
    if (features.isEmpty()) {
        return null
    }
    return buildJsonObject {
        put(
            key = TYPE,
            value = FEATURE_COLLECTION
        )
        put(
            key = FEATURES,
            element = JsonArray(content = features)
        )
    }.toString()
}

private fun feature(
    type: String,
    coordinates: JsonElement
): JsonObject =
    buildJsonObject {
        put(
            key = TYPE,
            value = FEATURE
        )
        put(
            key = PROPERTIES,
            element = JsonObject(content = emptyMap())
        )
        put(
            key = GEOMETRY,
            element = buildJsonObject {
                put(
                    key = TYPE,
                    value = type
                )
                put(
                    key = COORDINATES,
                    element = coordinates
                )
            }
        )
    }

private fun List<GeoPosition>.toCoordinates(): JsonArray = JsonArray(content = map { geoPosition: GeoPosition -> geoPosition.toCoordinate() })

private fun GeoPosition.toCoordinate(): JsonArray =
    JsonArray(
        content = listOf(
            JsonPrimitive(value = longitude),
            JsonPrimitive(value = latitude)
        )
    )