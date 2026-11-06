package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.model.RadarImage
import se.vadretapp.vadret.radar.usecase.DefaultObserveRadarTask
import se.vadretapp.vadret.radar.usecase.ObserveRadarTask
import se.vadretapp.vadret.testing.FixedClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val SAVED_AT: Instant = Instant.parse(input = "2026-10-24T13:00:00Z")

private val todayRadar = Radar(
    images = emptyList(),
    fetchedAt = SAVED_AT
)

private val archivedRadar = Radar(
    images = listOf(
        RadarImage(
            imageUrl = "https://smhi.se/archived.tif",
            validAt = Instant.parse(input = "2026-10-25T13:00:00Z")
        )
    ),
    fetchedAt = SAVED_AT
)

private class FakeRadarRepository : RadarRepository {

    val requestedImageUrls = mutableListOf<String>()

    var refreshes: Int = 0
    val observedDates = mutableListOf<LocalDate>()

    override fun observeRadar(date: LocalDate): Flow<Either<Failure, Radar?>> {
        observedDates.add(date)
        return flowOf(todayRadar.asRight())
    }

    override suspend fun refreshRadar(date: LocalDate): Either<Failure, Radar> {
        refreshes++
        return todayRadar.asRight()
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
    private val clock = FixedClock(now = Instant.parse(input = "2026-01-05T23:30:00Z"))
    private val observeRadarTask: ObserveRadarTask = DefaultObserveRadarTask(
        radarRepository = radarRepository,
        clock = clock
    )

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

    @Test
    fun `today is the day in UTC`() =
        runTest {
            observeRadarTask(imageUrl = null).first()

            assertEquals(
                expected = listOf(LocalDate.parse(input = "2026-01-05")),
                actual = radarRepository.observedDates
            )
        }
}