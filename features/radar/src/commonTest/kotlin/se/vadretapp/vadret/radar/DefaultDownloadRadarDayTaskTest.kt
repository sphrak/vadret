package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.model.SavedRadar
import kotlin.test.Test
import kotlin.test.assertEquals

private val imageUrls: List<String> = listOf(
    "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/11/02/radar_2611021400.tif",
    "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/11/02/radar_2611021405.tif"
)

private class CountingRadarRepository(
    val missingImageCount: Either<Failure, Int>,
    val downloadResult: Either<Failure, Unit> = Unit.asRight()
) : RadarRepository {

    val downloadedImageUrls = mutableListOf<List<String>>()

    override fun observeRadar(): Flow<Either<Failure, SavedRadar>> = error("Downloading a day never observes the radar")

    override suspend fun refreshRadar(): Either<Failure, SavedRadar> = error("Downloading a day never refreshes the radar")

    override suspend fun getRadarContaining(imageUrl: String): Either<Failure, Radar> = Radar(images = emptyList()).asRight()

    override suspend fun getRadarImage(imageUrl: String): Either<Failure, ByteArray> = ByteArray(size = 0).asRight()

    override suspend fun getMissingRadarImageCount(imageUrls: List<String>): Either<Failure, Int> = missingImageCount

    override suspend fun downloadRadarDays(imageUrls: List<String>): Either<Failure, Unit> {
        downloadedImageUrls.add(imageUrls)
        return downloadResult
    }
}

class DefaultDownloadRadarDayTaskTest {

    @Test
    fun `more than ten missing images download the day`() =
        runTest {
            val radarRepository = CountingRadarRepository(missingImageCount = 11.asRight())

            assertEquals(
                expected = Unit.asRight(),
                actual = DefaultDownloadRadarDayTask(radarRepository = radarRepository)(imageUrls = imageUrls)
            )
            assertEquals(
                expected = listOf(imageUrls),
                actual = radarRepository.downloadedImageUrls
            )
        }

    @Test
    fun `ten missing images are left to download one by one`() =
        runTest {
            val radarRepository = CountingRadarRepository(missingImageCount = 10.asRight())

            assertEquals(
                expected = Unit.asRight(),
                actual = DefaultDownloadRadarDayTask(radarRepository = radarRepository)(imageUrls = imageUrls)
            )
            assertEquals(
                expected = emptyList(),
                actual = radarRepository.downloadedImageUrls
            )
        }

    @Test
    fun `images that cannot be counted download nothing`() =
        runTest {
            val radarRepository = CountingRadarRepository(missingImageCount = Failure(errorCode = ErrorCode.RADAR_NOT_AVAILABLE).asLeft())

            assertEquals(
                expected = Failure(errorCode = ErrorCode.RADAR_NOT_AVAILABLE).asLeft(),
                actual = DefaultDownloadRadarDayTask(radarRepository = radarRepository)(imageUrls = imageUrls)
            )
            assertEquals(
                expected = emptyList(),
                actual = radarRepository.downloadedImageUrls
            )
        }

    @Test
    fun `a day that fails to download is a failure`() =
        runTest {
            val radarRepository = CountingRadarRepository(
                missingImageCount = 11.asRight(),
                downloadResult = Failure(errorCode = ErrorCode.NETWORK_FAILURE).asLeft()
            )

            assertEquals(
                expected = Failure(errorCode = ErrorCode.NETWORK_FAILURE).asLeft(),
                actual = DefaultDownloadRadarDayTask(radarRepository = radarRepository)(imageUrls = imageUrls)
            )
        }
}