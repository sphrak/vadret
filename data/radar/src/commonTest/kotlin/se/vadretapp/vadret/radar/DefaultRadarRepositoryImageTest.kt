package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.successOrNull
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import okio.Path
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.local.DefaultRadarImageLocalDataSource
import se.vadretapp.vadret.radar.local.DefaultRadarLocalDataSource
import se.vadretapp.vadret.radar.remote.RadarDto
import se.vadretapp.vadret.testing.FakeDiskCache
import se.vadretapp.vadret.testing.FixedClock
import se.vadretapp.vadret.testing.deleteRecursively
import se.vadretapp.vadret.testing.temporaryDirectory
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.Instant

private val now: Instant = Instant.parse(input = "2026-11-02T14:20:00Z")
private val today: LocalDate = LocalDate.parse(input = "2026-11-02")

private fun imageName(time: String): String = "radar_261102$time.tif"

private fun imageUrl(time: String): String =
    "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/11/02/${imageName(time = time)}"

private fun image(time: String): ByteArray = "radar image at $time".encodeToByteArray()

private fun archive(times: List<String>): ByteArray =
    zipArchive(
        files = times.map { time: String ->
            ArchivedFile(
                name = imageName(time = time),
                bytes = image(time = time)
            )
        }
    )

class DefaultRadarRepositoryImageTest {

    private val testScope = TestScope(StandardTestDispatcher())
    private val imageDirectory: Path = temporaryDirectory(prefix = "radar-repository-images")
    private val radarRemoteDataSource = FakeRadarRemoteDataSource(radarDto = RadarDto(files = emptyList()))
    private val radarRepository: RadarRepository = DefaultRadarRepository(
        radarRemoteDataSource = radarRemoteDataSource,
        radarLocalDataSource = DefaultRadarLocalDataSource(
            diskCache = FakeDiskCache(),
            coroutineDispatcher = StandardTestDispatcher(testScope.testScheduler)
        ),
        radarImageLocalDataSource = DefaultRadarImageLocalDataSource(
            directoryPath = imageDirectory.toString(),
            coroutineDispatcher = StandardTestDispatcher(testScope.testScheduler)
        ),
        clock = FixedClock(now = now)
    )

    @AfterTest
    fun tearDown() {
        imageDirectory.deleteRecursively()
    }

    private suspend fun imagesAt(times: List<String>): List<List<Byte>?> =
        times.map { time: String -> radarRepository.getRadarImage(imageUrl = imageUrl(time = time)).successOrNull?.toList() }

    private fun imagesOf(times: List<String>): List<List<Byte>> = times.map { time: String -> image(time = time).toList() }

    @Test
    fun `an image is downloaded on its own once and then read from disk`() =
        testScope.runTest {
            radarRemoteDataSource.images[imageName(time = "1410")] = image(time = "1410")

            val images: List<List<Byte>?> = imagesAt(times = listOf("1410", "1410"))

            assertEquals(
                expected = imagesOf(times = listOf("1410", "1410")),
                actual = images
            )
            assertEquals(
                expected = listOf("image 2026-11-02 radar_2611021410.tif"),
                actual = radarRemoteDataSource.downloads
            )
        }

    @Test
    fun `the same image asked for twice at once is downloaded once`() =
        testScope.runTest {
            radarRemoteDataSource.images[imageName(time = "1410")] = image(time = "1410")

            val images: List<Either<Failure, ByteArray>> = listOf("1410", "1410")
                .map { time: String -> async { radarRepository.getRadarImage(imageUrl = imageUrl(time = time)) } }
                .awaitAll()

            assertEquals(
                expected = imagesOf(times = listOf("1410", "1410")),
                actual = images.map { image: Either<Failure, ByteArray> -> image.successOrNull?.toList() }
            )
            assertEquals(
                expected = listOf("image 2026-11-02 radar_2611021410.tif"),
                actual = radarRemoteDataSource.downloads
            )
        }

    @Test
    fun `a downloaded day is read from disk without asking SMHI again`() =
        testScope.runTest {
            radarRemoteDataSource.archives[today] = archive(times = listOf("1400", "1405", "1410"))

            val download: Either<Failure, Unit> = radarRepository.downloadRadarDays(
                imageUrls = listOf("1400", "1405", "1410").map { time: String -> imageUrl(time = time) }
            )

            assertEquals(
                expected = Unit.asRight(),
                actual = download
            )
            assertEquals(
                expected = imagesOf(times = listOf("1400", "1405", "1410")),
                actual = imagesAt(times = listOf("1400", "1405", "1410"))
            )
            assertEquals(
                expected = listOf("archive 2026-11-02"),
                actual = radarRemoteDataSource.downloads
            )
        }

    @Test
    fun `an image asked for while its day downloads is read from the downloaded day`() =
        testScope.runTest {
            radarRemoteDataSource.archives[today] = archive(times = listOf("1400", "1405", "1410"))

            val download: Deferred<Either<Failure, Unit>> = async { radarRepository.downloadRadarDays(imageUrls = listOf(imageUrl(time = "1400"))) }
            val shownImage: Deferred<Either<Failure, ByteArray>> = async { radarRepository.getRadarImage(imageUrl = imageUrl(time = "1405")) }

            assertEquals(
                expected = Unit.asRight(),
                actual = download.await()
            )
            assertContentEquals(
                expected = image(time = "1405"),
                actual = shownImage.await().successOrNull
            )
            assertEquals(
                expected = listOf("archive 2026-11-02"),
                actual = radarRemoteDataSource.downloads
            )
        }

    @Test
    fun `images of two days download one archive for each day`() =
        testScope.runTest {
            val earlierDay: LocalDate = LocalDate.parse(input = "2026-11-01")
            radarRemoteDataSource.archives[today] = archive(times = listOf("1400"))
            radarRemoteDataSource.archives[earlierDay] = zipArchive(files = emptyList())

            radarRepository.downloadRadarDays(
                imageUrls = listOf(
                    "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/11/01/radar_2611012350.tif",
                    "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/11/01/radar_2611012355.tif",
                    imageUrl(time = "1400")
                )
            )

            assertEquals(
                expected = listOf("archive 2026-11-01", "archive 2026-11-02"),
                actual = radarRemoteDataSource.downloads
            )
        }

    @Test
    fun `missing images are the ones not yet on disk`() =
        testScope.runTest {
            radarRemoteDataSource.images[imageName(time = "1400")] = image(time = "1400")
            val imageUrls: List<String> = listOf("1400", "1405", "1410").map { time: String -> imageUrl(time = time) }
            val missingBefore: Either<Failure, Int> = radarRepository.getMissingRadarImageCount(imageUrls = imageUrls)

            imagesAt(times = listOf("1400"))

            assertEquals(
                expected = 3.asRight(),
                actual = missingBefore
            )
            assertEquals(
                expected = 2.asRight(),
                actual = radarRepository.getMissingRadarImageCount(imageUrls = imageUrls)
            )
        }

    @Test
    fun `a day that fails to download is a failure and leaves its images to be downloaded on their own`() =
        testScope.runTest {
            radarRemoteDataSource.images[imageName(time = "1400")] = image(time = "1400")

            withRecordedLogs {
                assertEquals(
                    expected = Failure(errorCode = ErrorCode.INVALID_RESPONSE).asLeft(),
                    actual = radarRepository.downloadRadarDays(imageUrls = listOf(imageUrl(time = "1400")))
                )
            }

            assertEquals(
                expected = imagesOf(times = listOf("1400")),
                actual = imagesAt(times = listOf("1400"))
            )
            assertEquals(
                expected = listOf("archive 2026-11-02", "image 2026-11-02 radar_2611021400.tif"),
                actual = radarRemoteDataSource.downloads
            )
        }

    @Test
    fun `an image url that is not an SMHI radar image is not downloaded`() =
        testScope.runTest {
            val imageUrls: List<String> = listOf(
                "https://smhi.se/1355.tif",
                "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/11/02/radar_2611021400.png",
                "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/11/02/../radar_2611021400.tif"
            )

            withRecordedLogs {
                assertEquals(
                    expected = imageUrls.map { Failure(errorCode = ErrorCode.RADAR_NOT_AVAILABLE).asLeft() },
                    actual = imageUrls.map { imageUrl: String -> radarRepository.getRadarImage(imageUrl = imageUrl) }
                )
                assertEquals(
                    expected = Failure(errorCode = ErrorCode.RADAR_NOT_AVAILABLE).asLeft(),
                    actual = radarRepository.getMissingRadarImageCount(imageUrls = imageUrls)
                )
                assertEquals(
                    expected = Failure(errorCode = ErrorCode.RADAR_NOT_AVAILABLE).asLeft(),
                    actual = radarRepository.downloadRadarDays(imageUrls = imageUrls)
                )
            }
            assertEquals(
                expected = emptyList(),
                actual = radarRemoteDataSource.downloads
            )
        }
}