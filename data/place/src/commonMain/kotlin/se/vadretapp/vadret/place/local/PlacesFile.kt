package se.vadretapp.vadret.place.local

private const val LINE_SEPARATOR = '\n'
private const val COLUMN_SEPARATOR = '\t'
private const val VERSION_LINES = 1
private const val LOCALITY_COLUMN = 0
private const val MUNICIPALITY_COLUMN = 1
private const val COUNTY_COLUMN = 2
private const val LATITUDE_COLUMN = 3
private const val LONGITUDE_COLUMN = 4
private const val POPULATION_COLUMN = 5
private const val COLUMN_COUNT = 6

// Searching for "malmo" or "upplands bro" finds Malmö and Upplands-Bro, so letters lose their accents and a hyphen
// parts words as a space does.
private val FOLDED_CHARACTERS: Map<Char, Char> = mapOf(
    'å' to 'a',
    'ä' to 'a',
    'á' to 'a',
    'à' to 'a',
    'ö' to 'o',
    'ø' to 'o',
    'ó' to 'o',
    'é' to 'e',
    'è' to 'e',
    'ü' to 'u',
    '-' to ' '
)

// The first line names the version of the places, so a changed list is told apart without reading every place.
internal fun String.seedVersion(): String {
    val seedVersion: String = substringBefore(delimiter = LINE_SEPARATOR).trim()
    require(value = seedVersion.isNotEmpty()) { "The places have no version" }
    return seedVersion
}

internal fun String.placeEntities(): List<PlaceEntity> =
    lineSequence()
        .drop(n = VERSION_LINES)
        .filter { line: String -> line.isNotBlank() }
        .map { line: String -> line.toPlaceEntity() }
        .toList()

internal fun String.folded(): String =
    buildString {
        for (character: Char in this@folded.lowercase()) {
            append(FOLDED_CHARACTERS[character] ?: character)
        }
    }

private fun String.toPlaceEntity(): PlaceEntity {
    val columns: List<String> = split(COLUMN_SEPARATOR)
    require(value = columns.size == COLUMN_COUNT) { "Expected $COLUMN_COUNT columns in $this" }
    return PlaceEntity(
        locality = columns[LOCALITY_COLUMN],
        municipality = columns[MUNICIPALITY_COLUMN],
        county = columns[COUNTY_COLUMN],
        latitude = columns[LATITUDE_COLUMN].toDouble(),
        longitude = columns[LONGITUDE_COLUMN].toDouble(),
        population = columns[POPULATION_COLUMN].toInt(),
        foldedLocality = columns[LOCALITY_COLUMN].folded()
    )
}