package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.model.RadarImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val todayRadar = Radar(images = emptyList())

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

    override suspend fun getRadar(): Either<Failure, Radar> = todayRadar.asRight()

    override suspend fun getRadarContaining(imageUrl: String): Either<Failure, Radar> {
        requestedImageUrls.add(imageUrl)
        return archivedRadar.asRight()
    }

    override suspend fun getRadarImage(imageUrl: String): Either<Failure, ByteArray> = ByteArray(size = 0).asRight()

    override suspend fun getMissingRadarImageCount(imageUrls: List<String>): Either<Failure, Int> = 0.asRight()

    override suspend fun downloadRadarDays(imageUrls: List<String>): Either<Failure, Unit> = Unit.asRight()
}

class DefaultGetRadarTaskTest {

    private val radarRepository = FakeRadarRepository()
    private val getRadarTask: GetRadarTask = DefaultGetRadarTask(radarRepository = radarRepository)

    @Test
    fun `without an image the radar of today is loaded`() =
        runTest {
            assertEquals<Either<Failure, Radar>>(
                expected = todayRadar.asRight(),
                actual = getRadarTask(imageUrl = null)
            )
            assertEquals(expected = emptyList(), actual = radarRepository.requestedImageUrls)
        }

    @Test
    fun `an image loads the radar that contains it`() =
        runTest {
            assertEquals<Either<Failure, Radar>>(
                expected = archivedRadar.asRight(),
                actual = getRadarTask(imageUrl = "https://smhi.se/archived.tif")
            )
            assertEquals(expected = listOf("https://smhi.se/archived.tif"), actual = radarRepository.requestedImageUrls)
        }
}