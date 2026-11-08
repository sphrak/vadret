package se.vadretapp.vadret.place.local

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val BUNDLED_PLACES = "../../core/di/src/commonMain/composeResources/files/places.tsv"
private const val SWEDISH_MUNICIPALITIES = 290
private const val SWEDISH_COUNTIES = 21
private const val LIMIT = 10

class BundledPlacesTest {

    private val testScope = TestScope(StandardTestDispatcher())
    private val placeDatabase: PlaceDatabase = inMemoryPlaceDatabase(
        coroutineDispatcher = StandardTestDispatcher(testScope.testScheduler)
    )
    private val places: String = SystemFileSystem.source(path = Path(BUNDLED_PLACES)).buffered().use { source -> source.readString() }
    private val placeLocalDataSource: PlaceLocalDataSource = DefaultPlaceLocalDataSource(
        placeDatabase = placeDatabase,
        readPlaces = { places }
    )

    @AfterTest
    fun tearDown() {
        placeDatabase.close()
    }

    @Test
    fun `the bundled places name every municipality and county of Sweden once`() {
        val placeEntities: List<PlaceEntity> = places.placeEntities()

        assertEquals(
            expected = Triple(placeEntities.size, SWEDISH_MUNICIPALITIES, SWEDISH_COUNTIES),
            actual = Triple(
                placeEntities.distinctBy { placeEntity: PlaceEntity -> placeEntity.locality to placeEntity.municipality }.size,
                placeEntities.map { placeEntity: PlaceEntity -> placeEntity.municipality }.toSet().size,
                placeEntities.map { placeEntity: PlaceEntity -> placeEntity.county }.toSet().size
            )
        )
    }

    @Test
    fun `a search finds the town people mean first`() =
        testScope.runTest {
            val queries: List<String> = listOf("stockholm", "goteborg", "lund", "malmo", "visby", "upplands", "sto")

            assertEquals(
                expected = listOf(
                    "Stockholm, Stockholm, Stockholm",
                    "Göteborg, Göteborg, Västra Götaland",
                    "Lund, Lund, Skåne",
                    "Malmö, Malmö, Skåne",
                    "Visby, Gotland, Gotland",
                    "Upplands Väsby, Upplands Väsby, Stockholm",
                    "Stockholm, Stockholm, Stockholm"
                ),
                actual = queries.map { query: String ->
                    placeLocalDataSource
                        .search(
                            query = query,
                            limit = LIMIT
                        ).first()
                        .description()
                }
            )
        }
}

private fun PlaceEntity.description(): String = "$locality, $municipality, $county"