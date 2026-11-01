package se.vadretapp.vadret.aggregatedfeed.extension

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import se.vadretapp.vadret.aggregatedfeed.local.GeoLineEntity
import se.vadretapp.vadret.aggregatedfeed.local.GeoPolygonEntity
import se.vadretapp.vadret.aggregatedfeed.local.GeoPositionEntity
import se.vadretapp.vadret.aggregatedfeed.remote.GeoJsonDto
import se.vadretapp.vadret.aggregatedfeed.remote.GeoJsonGeometryDto

private const val POINT = "Point"
private const val MULTI_POINT = "MultiPoint"
private const val LINE_STRING = "LineString"
private const val MULTI_LINE_STRING = "MultiLineString"
private const val POLYGON = "Polygon"
private const val MULTI_POLYGON = "MultiPolygon"
private const val LONGITUDE_INDEX = 0
private const val LATITUDE_INDEX = 1
private const val MINIMUM_POSITION_SIZE = 2
private const val MINIMUM_RING_SIZE = 4

internal fun GeoJsonDto.toGeoPolygonEntities(): List<GeoPolygonEntity> =
    geometries().flatMap { geometryDto: GeoJsonGeometryDto ->
        when (geometryDto.type) {
            POLYGON -> listOfNotNull(geometryDto.coordinates.toGeoPolygonEntity())
            MULTI_POLYGON -> geometryDto.coordinates.elements().mapNotNull { polygon: JsonElement -> polygon.toGeoPolygonEntity() }
            else -> emptyList()
        }
    }

internal fun GeoJsonDto.toGeoLineEntities(): List<GeoLineEntity> =
    geometries().flatMap { geometryDto: GeoJsonGeometryDto ->
        when (geometryDto.type) {
            POINT -> listOfNotNull(geometryDto.coordinates.toGeoPositionEntity()).toGeoLineEntities()
            MULTI_POINT -> geometryDto.coordinates.toGeoPositionEntities().toGeoLineEntities()
            LINE_STRING -> geometryDto.coordinates.toGeoPositionEntities().toGeoLineEntities()
            MULTI_LINE_STRING ->
                geometryDto.coordinates
                    .elements()
                    .flatMap { line: JsonElement -> line.toGeoPositionEntities().toGeoLineEntities() }
            else -> emptyList()
        }
    }

internal fun GeoJsonDto.unsupportedGeometryTypes(): Set<String> =
    geometries()
        .map { geometryDto: GeoJsonGeometryDto -> geometryDto.type }
        .filterNot { type: String -> type in supportedGeometryTypes }
        .toSet()

private val supportedGeometryTypes: Set<String> = setOf(POINT, MULTI_POINT, LINE_STRING, MULTI_LINE_STRING, POLYGON, MULTI_POLYGON)

private fun GeoJsonDto.geometries(): List<GeoJsonGeometryDto> = listOfNotNull(geometry) + features.orEmpty().flatMap { geoJsonDto: GeoJsonDto -> geoJsonDto.geometries() }

private fun JsonElement?.elements(): List<JsonElement> = (this as? JsonArray).orEmpty()

private fun JsonElement?.toGeoPolygonEntity(): GeoPolygonEntity? {
    val rings: List<List<GeoPositionEntity>> = elements()
        .map { ring: JsonElement -> ring.toGeoPositionEntities() }
        .filter { ring: List<GeoPositionEntity> -> ring.size >= MINIMUM_RING_SIZE }
    val outline: List<GeoPositionEntity> = rings.firstOrNull() ?: return null
    return GeoPolygonEntity(
        outline = outline,
        holes = rings.drop(n = 1)
    )
}

private fun JsonElement?.toGeoPositionEntities(): List<GeoPositionEntity> = elements().mapNotNull { position: JsonElement -> position.toGeoPositionEntity() }

private fun JsonElement?.toGeoPositionEntity(): GeoPositionEntity? {
    val values: List<Double> = elements().mapNotNull { value: JsonElement -> (value as? JsonPrimitive)?.doubleOrNull }
    if (values.size < MINIMUM_POSITION_SIZE) {
        return null
    }
    return GeoPositionEntity(
        latitude = values[LATITUDE_INDEX],
        longitude = values[LONGITUDE_INDEX]
    )
}

private fun List<GeoPositionEntity>.toGeoLineEntities(): List<GeoLineEntity> =
    if (isEmpty()) {
        emptyList()
    } else {
        listOf(GeoLineEntity(positions = this))
    }