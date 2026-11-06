package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.model.RadarImage
import se.vadretapp.vadret.radar.model.SavedRadar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val todayRadar = Radar(images = emptyList())

private val SAVED_AT: Instant = Instant.parse(input = "2026-10-24T13:00:00Z")

private val archivedRadar = Radar(
    images = listOf(
        RadarImage(
            imageUrl = "https://smhi.se/archived.tif",
            validAt = Instant.parse(input = "2026-10-25T13:00:00Z")
        )
    )
)

private class FakeRadarRepository : RadarRepository {

    val requestedImageUrls = mutableListOf<String>()

    var refreshes: Int = 0

    override fun observeRadar(): Flow<Either<Failure, SavedRadar>> =
        flowOf(
            SavedRadar(
                radar = todayRadar,
                fetchedAt = SAVED_AT
            ).asRight()
        )

    override suspend fun refreshRadar(): Either<Failure, SavedRadar> {
        refreshes++
        return SavedRadar(
            radar = todayRadar,
            fetchedAt = SAVED_AT
        ).asRight()
    }

    override suspend fun getRadarContaining(imageUrl: String): Either<Failure, Radar> {
        requestedImageUrls.add(imageUrl)
        return archivedRadar.asRight()
    }

    override suspend fun getRadarImage(imageUrl: String): Either<Failure, ByteArray> = ByteArray(size = 0).asRight()

    override suspend fun getMissingRadarImageCount(imageUrls: List<String>): Either<Failure, Int> = 0.asRight()

    override suspend fun downloadRadarDays(imageUrls: List<String>): Either<Failure, Unit> = Unit.asRight()
}

class DefaultObserveRadarTaskTest {

    private val radarRepository = FakeRadarRepository()
    private val observeRadarTask: ObserveRadarTask = DefaultObserveRadarTask(radarRepository = radarRepository)

    @Test
    fun `without an image the radar of today is loaded`() =
        runTest {
            assertEquals<Either<Failure, Radar>>(
                expected = todayRadar.asRight(),
                actual = observeRadarTask(imageUrl = null).first()
            )
            assertEquals(expected = emptyList<String>() to 1, actual = radarRepository.requestedImageUrls to radarRepository.refreshes)
        }

    @Test
    fun `an image loads the radar that contains it`() =
        runTest {
            assertEquals<Either<Failure, Radar>>(
                expected = archivedRadar.asRight(),
                actual = observeRadarTask(imageUrl = "https://smhi.se/archived.tif").first()
            )
            assertEquals(expected = listOf("https://smhi.se/archived.tif") to 0, actual = radarRepository.requestedImageUrls to radarRepository.refreshes)
        }
}