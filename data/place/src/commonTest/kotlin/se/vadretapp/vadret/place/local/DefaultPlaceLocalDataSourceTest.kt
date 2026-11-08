package se.vadretapp.vadret.place.local

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val PLACES = """first
Lund	Lund	Skåne	55.70584	13.19321	87244
Lund	Avesta	Dalarna	60.15	16.3	0
Lunden	Ale	Västra Götaland	57.95	12.1	0
Lundby	Göteborg	Västra Götaland	57.71	11.93	2000
Norra Lund	Avesta	Dalarna	60.2	16.4	0
Malmö	Malmö	Skåne	55.60587	13.00073	362133
Upplands-Bro	Upplands-Bro	Stockholm	59.51	17.63	100
Örebro	Örebro	Örebro	59.27412	15.2066	98573
Orsa	Orsa	Dalarna	61.12	14.62	5000
Ab_c	Ale	Västra Götaland	57.9	12.1	0
"""

private const val SECOND_PLACES = """second
Kiruna	Kiruna	Norrbotten	67.85572	20.22513	22841
"""

private const val LIMIT = 10

class DefaultPlaceLocalDataSourceTest {

    private val testScope = TestScope(StandardTestDispatcher())
    private val placeDatabase: PlaceDatabase = inMemoryPlaceDatabase(
        coroutineDispatcher = StandardTestDispatcher(testScope.testScheduler)
    )
    private var readCount = 0

    @AfterTest
    fun tearDown() {
        placeDatabase.close()
    }

    private fun dataSource(places: String = PLACES): PlaceLocalDataSource =
        DefaultPlaceLocalDataSource(
            placeDatabase = placeDatabase,
            readPlaces = {
                readCount++
                places
            }
        )

    private suspend fun PlaceLocalDataSource.searchNames(
        query: String,
        limit: Int = LIMIT
    ): List<String> =
        search(
            query = query,
            limit = limit
        ).map { placeEntity: PlaceEntity -> "${placeEntity.locality}, ${placeEntity.municipality}" }

    @Test
    fun `the place named as the query comes first then places starting with it then a later word and the most populated first`() =
        testScope.runTest {
            assertEquals(
                expected = listOf("Lund, Lund", "Lund, Avesta", "Lundby, Göteborg", "Lunden, Ale", "Norra Lund, Avesta"),
                actual = dataSource().searchNames(query = "lund")
            )
        }

    @Test
    fun `case accents and hyphens do not matter`() =
        testScope.runTest {
            val placeLocalDataSource: PlaceLocalDataSource = dataSource()

            assertEquals(
                expected = listOf(
                    listOf("Malmö, Malmö"),
                    listOf("Malmö, Malmö"),
                    listOf("Upplands-Bro, Upplands-Bro"),
                    listOf("Upplands-Bro, Upplands-Bro"),
                    listOf("Lund, Lund", "Lund, Avesta", "Lundby, Göteborg", "Lunden, Ale", "Norra Lund, Avesta")
                ),
                actual = listOf("malmo", "MALMÖ", "upplands bro", "bro", "  LUND ").map { query: String ->
                    placeLocalDataSource.searchNames(query = query)
                }
            )
        }

    @Test
    fun `a letter without accent also finds the accented one and the more populated first`() =
        testScope.runTest {
            assertEquals(
                expected = listOf("Örebro, Örebro", "Orsa, Orsa"),
                actual = dataSource().searchNames(query = "o")
            )
        }

    @Test
    fun `wildcards are searched for as they are`() =
        testScope.runTest {
            val placeLocalDataSource: PlaceLocalDataSource = dataSource()

            assertEquals(
                expected = listOf(emptyList(), listOf("Ab_c, Ale"), emptyList()),
                actual = listOf("%", "ab_", "a\\").map { query: String -> placeLocalDataSource.searchNames(query = query) }
            )
        }

    @Test
    fun `at most limit places are found`() =
        testScope.runTest {
            assertEquals(
                expected = listOf("Lund, Lund", "Lund, Avesta"),
                actual = dataSource().searchNames(
                    query = "lund",
                    limit = 2
                )
            )
        }

    @Test
    fun `a found place keeps every column`() =
        testScope.runTest {
            assertEquals(
                expected = listOf(
                    PlaceEntity(
                        locality = "Upplands-Bro",
                        municipality = "Upplands-Bro",
                        county = "Stockholm",
                        latitude = 59.51,
                        longitude = 17.63,
                        population = 100,
                        foldedLocality = "upplands bro"
                    )
                ),
                actual = dataSource().search(
                    query = "Upplands",
                    limit = LIMIT
                )
            )
        }

    @Test
    fun `the places are read once`() =
        testScope.runTest {
            val placeLocalDataSource: PlaceLocalDataSource = dataSource()

            placeLocalDataSource.search(query = "Lund", limit = LIMIT)
            placeLocalDataSource.search(query = "Malmö", limit = LIMIT)

            assertEquals(expected = 1, actual = readCount)
        }

    @Test
    fun `places of the same version are not written again`() =
        testScope.runTest {
            dataSource().search(query = "Lund", limit = LIMIT)

            val placesOfTheSameVersion: String = PLACES.replace(oldValue = "Malmö\tMalmö", newValue = "Malmo\tMalmö")

            assertEquals(
                expected = listOf("Malmö, Malmö"),
                actual = dataSource(places = placesOfTheSameVersion).searchNames(query = "Malm")
            )
        }

    @Test
    fun `places of a new version replace the old ones`() =
        testScope.runTest {
            dataSource().search(query = "Lund", limit = LIMIT)

            val placeLocalDataSource: PlaceLocalDataSource = dataSource(places = SECOND_PLACES)

            assertEquals(
                expected = listOf(emptyList(), listOf("Kiruna, Kiruna")),
                actual = listOf("Lund", "Kiruna").map { query: String -> placeLocalDataSource.searchNames(query = query) }
            )
            assertEquals(expected = 1, actual = placeDatabase.placeDao().countSeeds(seedVersion = "second"))
            assertEquals(expected = 0, actual = placeDatabase.placeDao().countSeeds(seedVersion = "first"))
        }

    @Test
    fun `places that cannot be read throw and leave nothing written`() =
        testScope.runTest {
            val unreadablePlaces: List<String> = listOf(
                "",
                "first\nLund\tLund\tSkåne\t55.7\t13.2",
                "first\nLund\tLund\tSkåne\tnorth\t13.2\t0",
                "first\nLund\tLund\tSkåne\t55.7\t13.2\tmany"
            )

            unreadablePlaces.forEach { places: String ->
                assertFailsWith<IllegalArgumentException> {
                    dataSource(places = places).search(query = "Lund", limit = LIMIT)
                }
            }
            assertEquals(expected = 0, actual = placeDatabase.placeDao().countSeeds(seedVersion = "first"))
        }

    @Test
    fun `the same place twice in one municipality fails the whole seed`() =
        testScope.runTest {
            val doubledPlaces = "first\nLund\tLund\tSkåne\t55.7\t13.2\t1\nLund\tLund\tSkåne\t55.8\t13.3\t2\n"

            assertFailsWith<SQLiteException> {
                dataSource(places = doubledPlaces).search(query = "Lund", limit = LIMIT)
            }
            assertEquals(expected = 0, actual = placeDatabase.placeDao().countSeeds(seedVersion = "first"))
        }
}