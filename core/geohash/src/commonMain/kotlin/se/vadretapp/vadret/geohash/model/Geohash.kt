package se.vadretapp.vadret.geohash.model

import kotlin.jvm.JvmInline

@JvmInline
value class Geohash(val value: String) {
    init {
        require(value = value.length == GEOHASH_LENGTH && value.all { character -> character in GEOHASH_ALPHABET }) {
            "A geohash is $GEOHASH_LENGTH characters of the geohash alphabet, was: $value"
        }
    }

    fun isInSwedishWaters(): Boolean {
        var southLatitude = -MAXIMUM_LATITUDE
        var northLatitude = MAXIMUM_LATITUDE
        var westLongitude = -MAXIMUM_LONGITUDE
        var eastLongitude = MAXIMUM_LONGITUDE
        var isLongitudeBit = true
        for (character in value) {
            val alphabetIndex = GEOHASH_ALPHABET.indexOf(char = character)
            for (shift in BITS_PER_CHARACTER - 1 downTo 0) {
                val isUpperHalf = ((alphabetIndex shr shift) and UPPER_HALF_BIT) == UPPER_HALF_BIT
                if (isLongitudeBit) {
                    val middle = (westLongitude + eastLongitude) / 2
                    if (isUpperHalf) {
                        westLongitude = middle
                    } else {
                        eastLongitude = middle
                    }
                } else {
                    val middle = (southLatitude + northLatitude) / 2
                    if (isUpperHalf) {
                        southLatitude = middle
                    } else {
                        northLatitude = middle
                    }
                }
                isLongitudeBit = !isLongitudeBit
            }
        }
        return southLatitude <= SWEDISH_WATERS_NORTH_LATITUDE &&
            northLatitude >= SWEDISH_WATERS_SOUTH_LATITUDE &&
            westLongitude <= SWEDISH_WATERS_EAST_LONGITUDE &&
            eastLongitude >= SWEDISH_WATERS_WEST_LONGITUDE
    }
}

fun geohashOf(
    latitude: Double,
    longitude: Double
): Geohash? {
    if (latitude !in -MAXIMUM_LATITUDE..MAXIMUM_LATITUDE || longitude !in -MAXIMUM_LONGITUDE..MAXIMUM_LONGITUDE) {
        return null
    }
    var southLatitude = -MAXIMUM_LATITUDE
    var northLatitude = MAXIMUM_LATITUDE
    var westLongitude = -MAXIMUM_LONGITUDE
    var eastLongitude = MAXIMUM_LONGITUDE
    var isLongitudeBit = true
    var bitsInCharacter = 0
    var alphabetIndex = 0
    val geohash = StringBuilder()
    while (geohash.length < GEOHASH_LENGTH) {
        val isUpperHalf = if (isLongitudeBit) {
            val middle = (westLongitude + eastLongitude) / 2
            val isEast = longitude >= middle
            if (isEast) {
                westLongitude = middle
            } else {
                eastLongitude = middle
            }
            isEast
        } else {
            val middle = (southLatitude + northLatitude) / 2
            val isNorth = latitude >= middle
            if (isNorth) {
                southLatitude = middle
            } else {
                northLatitude = middle
            }
            isNorth
        }
        alphabetIndex = if (isUpperHalf) {
            (alphabetIndex shl 1) or UPPER_HALF_BIT
        } else {
            alphabetIndex shl 1
        }
        isLongitudeBit = !isLongitudeBit
        bitsInCharacter++
        if (bitsInCharacter == BITS_PER_CHARACTER) {
            geohash.append(GEOHASH_ALPHABET[alphabetIndex])
            bitsInCharacter = 0
            alphabetIndex = 0
        }
    }
    return Geohash(value = geohash.toString())
}

private const val GEOHASH_LENGTH = 5
private const val GEOHASH_ALPHABET = "0123456789bcdefghjkmnpqrstuvwxyz"
private const val BITS_PER_CHARACTER = 5
private const val UPPER_HALF_BIT = 1
private const val MAXIMUM_LATITUDE = 90.0
private const val MAXIMUM_LONGITUDE = 180.0
private const val SWEDISH_WATERS_SOUTH_LATITUDE = 54.5
private const val SWEDISH_WATERS_NORTH_LATITUDE = 69.5
private const val SWEDISH_WATERS_WEST_LONGITUDE = 10.0
private const val SWEDISH_WATERS_EAST_LONGITUDE = 24.5