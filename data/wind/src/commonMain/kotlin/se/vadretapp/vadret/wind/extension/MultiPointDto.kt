package se.vadretapp.vadret.wind.extension

import se.vadretapp.vadret.wind.exception.InvalidWindGridException
import se.vadretapp.vadret.wind.remote.MultiPointDto

private const val COORDINATE_SIZE = 2
private const val LONGITUDE_INDEX = 0
private const val LATITUDE_INDEX = 1

internal fun MultiPointDto.columnCount(): Int {
    if (coordinates.any { coordinate: List<Double> -> coordinate.size != COORDINATE_SIZE }) {
        throw InvalidWindGridException(message = "Wind grid coordinates are not longitude and latitude pairs")
    }
    val longitudes: List<Double> = longitudes()
    val columnCount: Int = (1 until longitudes.size)
        .firstOrNull { index: Int -> longitudes[index] < longitudes[index - 1] } ?: longitudes.size
    val isRowAligned: Boolean = longitudes.isNotEmpty() &&
        longitudes.size % columnCount == 0 &&
        (1 until longitudes.size).all { index: Int -> (longitudes[index] < longitudes[index - 1]) == (index % columnCount == 0) }
    if (!isRowAligned) {
        throw InvalidWindGridException(message = "Wind grid of ${longitudes.size} points has no regular rows")
    }
    return columnCount
}

internal fun MultiPointDto.longitudes(): List<Double> = coordinates.map { coordinate: List<Double> -> coordinate[LONGITUDE_INDEX] }

internal fun MultiPointDto.latitudes(): List<Double> = coordinates.map { coordinate: List<Double> -> coordinate[LATITUDE_INDEX] }