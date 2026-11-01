package se.vadretapp.vadret.wind.model

data class WindGrid(
    val columnCount: Int,
    val rowCount: Int,
    val longitudes: List<Double>,
    val latitudes: List<Double>
)