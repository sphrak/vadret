package se.vadretapp.vadret.warning.extension

import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.FeedArea
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegion
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegionKind
import se.vadretapp.vadret.aggregatedfeed.model.GeoLine
import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.GeoPosition
import se.vadretapp.vadret.warning.geo.contains
import se.vadretapp.vadret.warning.geo.distanceInMetersTo
import se.vadretapp.vadret.warning.geo.isSamePlaceAs
import se.vadretapp.vadret.warning.model.WarningPlace

private const val LAND_RADIUS_IN_METERS = 2_000.0
private const val SEA_RADIUS_IN_METERS = 0.0

internal fun AggregatedFeed.affects(warningPlace: WarningPlace): Boolean =
    if (area.polygons.isEmpty() && area.lines.isEmpty()) {
        area.regions.any { feedRegion: FeedRegion -> feedRegion.covers(warningPlace = warningPlace) }
    } else {
        area.reaches(
            position = GeoPosition(
                latitude = warningPlace.latitude,
                longitude = warningPlace.longitude
            )
        )
    }

private fun FeedArea.reaches(position: GeoPosition): Boolean {
    val radiusInMeters: Double = if (isAtSea()) {
        SEA_RADIUS_IN_METERS
    } else {
        LAND_RADIUS_IN_METERS
    }
    return polygons.any { geoPolygon: GeoPolygon -> geoPolygon.contains(position = position) } ||
        polygons.any { geoPolygon: GeoPolygon ->
            (listOf(geoPolygon.outline) + geoPolygon.holes).any { ring: List<GeoPosition> ->
                position.distanceInMetersTo(path = ring) <= radiusInMeters
            }
        } ||
        lines.any { geoLine: GeoLine -> position.distanceInMetersTo(path = geoLine.positions) <= radiusInMeters }
}

private fun FeedArea.isAtSea(): Boolean = regions.isNotEmpty() && regions.all { feedRegion: FeedRegion -> feedRegion.kind == FeedRegionKind.SEA }

private fun FeedRegion.covers(warningPlace: WarningPlace): Boolean {
    val placeName: String? = when (kind) {
        FeedRegionKind.COUNTY -> warningPlace.county
        FeedRegionKind.MUNICIPALITY -> warningPlace.municipality
        FeedRegionKind.COUNTRY,
        FeedRegionKind.SEA,
        FeedRegionKind.OTHER -> null
    }
    return placeName != null && placeName.isSamePlaceAs(other = name)
}