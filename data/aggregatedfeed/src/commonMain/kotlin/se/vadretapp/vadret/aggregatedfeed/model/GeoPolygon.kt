package se.vadretapp.vadret.aggregatedfeed.model

data class GeoPolygon(
    val outline: List<GeoPosition>,
    val holes: List<List<GeoPosition>>
)