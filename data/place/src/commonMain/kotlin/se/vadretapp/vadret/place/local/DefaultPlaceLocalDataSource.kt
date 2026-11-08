package se.vadretapp.vadret.place.local

import androidx.room3.withWriteTransaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.atomics.AtomicBoolean

private const val LIKE_ESCAPE = "\\"
private val LIKE_WILDCARDS: List<String> = listOf("%", "_")

internal class DefaultPlaceLocalDataSource(
    private val placeDatabase: PlaceDatabase,
    private val readPlaces: suspend () -> String
) : PlaceLocalDataSource {

    private val placeDao: PlaceDao = placeDatabase.placeDao()
    private val seedMutex: Mutex = Mutex()
    private val isSeeded: AtomicBoolean = AtomicBoolean(value = false)

    override suspend fun search(
        query: String,
        limit: Int
    ): List<PlaceEntity> {
        seed()
        val foldedQuery: String = query.trim().folded()
        val likeQuery: String = foldedQuery.escapedForLike()
        return placeDao.search(
            foldedQuery = foldedQuery,
            prefixPattern = "$likeQuery%",
            wordPattern = "% $likeQuery%",
            limit = limit
        )
    }

    // The bundled places are written once for each version of them, so an app update with new places replaces the
    // old ones while later searches never read them again.
    private suspend fun seed() {
        if (isSeeded.load()) {
            return
        }
        seedMutex.withLock {
            if (!isSeeded.load()) {
                val places: String = readPlaces()
                val seedVersion: String = places.seedVersion()
                if (placeDao.countSeeds(seedVersion = seedVersion) == 0) {
                    replacePlaces(
                        seedVersion = seedVersion,
                        placeEntities = places.placeEntities()
                    )
                }
                isSeeded.store(newValue = true)
            }
        }
    }

    private suspend fun replacePlaces(
        seedVersion: String,
        placeEntities: List<PlaceEntity>
    ) {
        placeDatabase.withWriteTransaction {
            placeDao.deletePlaces()
            placeDao.deleteSeeds()
            placeDao.insertPlaces(placeEntities = placeEntities)
            placeDao.insertSeed(placeSeedEntity = PlaceSeedEntity(seedVersion = seedVersion))
        }
    }
}

// What someone types is searched for as it is, so the wildcards of LIKE lose their meaning.
private fun String.escapedForLike(): String =
    (listOf(LIKE_ESCAPE) + LIKE_WILDCARDS).fold(initial = this) { escaped: String, special: String ->
        escaped.replace(oldValue = special, newValue = "$LIKE_ESCAPE$special")
    }