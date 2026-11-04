package se.vadretapp.vadret.radar

import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import okio.Buffer
import se.vadretapp.vadret.radar.exception.RadarDownloadException
import se.vadretapp.vadret.radar.remote.DefaultRadarRemoteDataSource
import se.vadretapp.vadret.radar.remote.RadarRemoteDataSource
import se.vadretapp.vadret.testing.mockHttpClient
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val IMAGE_ABOVE_LIMIT_BYTES = 1_048_577
private const val ARCHIVE_LIMIT_BYTES = 67_108_864L
private const val ARCHIVE_ABOVE_LIMIT_BYTES = 67_108_865

private val fifthOfJanuary: LocalDate = LocalDate.parse(input = "2026-01-05")
private val radarImage: ByteArray = "radar image".encodeToByteArray()

class DefaultRadarRemoteDataSourceTest {

    private val requests = mutableListOf<HttpRequestData>()

    private val archiveSink = Buffer()

    private fun radarRemoteDataSource(
        responseBody: ByteArray,
        responseHeaders: Headers = headersOf()
    ): RadarRemoteDataSource =
        DefaultRadarRemoteDataSource(
            httpClient = lazy {
                mockHttpClient { request: HttpRequestData ->
                    requests.add(request)
                    respond(
                        content = responseBody,
                        headers = responseHeaders
                    )
                }
            }
        )

    private fun radarRemoteDataSource(responseBody: ByteReadChannel): RadarRemoteDataSource =
        DefaultRadarRemoteDataSource(
            httpClient = lazy {
                mockHttpClient { request: HttpRequestData ->
                    requests.add(request)
                    respond(content = responseBody)
                }
            }
        )

    @Test
    fun `the archive of the UTC day is asked for as GeoTIFF of any type and kept out of the HTTP cache`() =
        runTest {
            radarRemoteDataSource(responseBody = radarImage).getRadarArchive(
                date = fifthOfJanuary,
                archiveSink = archiveSink
            )

            assertContentEquals(
                expected = radarImage,
                actual = archiveSink.readByteArray()
            )
            assertEquals(
                expected = "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/1/5.zip?format=tif&timeZone=GMT",
                actual = requests.single().url.toString()
            )
            assertEquals(
                expected = "no-store",
                actual = requests.single().headers[HttpHeaders.CacheControl]
            )
            assertTrue(actual = "*/*" in requests.single().headers.getAll(HttpHeaders.Accept).orEmpty())
        }

    @Test
    fun `a single image is asked for by its day and name as any type and kept out of the HTTP cache`() =
        runTest {
            assertContentEquals(
                expected = radarImage,
                actual = radarRemoteDataSource(responseBody = radarImage).getRadarImage(
                    date = fifthOfJanuary,
                    imageName = "radar_2601052330.tif"
                )
            )
            assertEquals(
                expected = "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/1/5/radar_2601052330.tif",
                actual = requests.single().url.toString()
            )
            assertEquals(
                expected = "no-store",
                actual = requests.single().headers[HttpHeaders.CacheControl]
            )
            assertTrue(actual = "*/*" in requests.single().headers.getAll(HttpHeaders.Accept).orEmpty())
        }

    @Test
    fun `an image larger than a radar image can be is refused`() =
        runTest {
            assertFailsWith<RadarDownloadException> {
                radarRemoteDataSource(responseBody = ByteArray(size = IMAGE_ABOVE_LIMIT_BYTES)).getRadarImage(
                    date = fifthOfJanuary,
                    imageName = "radar_2601052330.tif"
                )
            }
        }

    @Test
    fun `an archive of unknown length streams to its sink`() =
        runTest {
            radarRemoteDataSource(responseBody = ByteReadChannel(content = radarImage)).getRadarArchive(
                date = fifthOfJanuary,
                archiveSink = archiveSink
            )

            assertContentEquals(
                expected = radarImage,
                actual = archiveSink.readByteArray()
            )
        }

    @Test
    fun `an archive that declares more than a day can hold is refused before it is read`() =
        runTest {
            assertFailsWith<RadarDownloadException> {
                radarRemoteDataSource(
                    responseBody = radarImage,
                    responseHeaders = headersOf(
                        name = HttpHeaders.ContentLength,
                        value = ARCHIVE_ABOVE_LIMIT_BYTES.toString()
                    )
                ).getRadarArchive(
                    date = fifthOfJanuary,
                    archiveSink = archiveSink
                )
            }
            assertEquals(
                expected = 0L,
                actual = archiveSink.size
            )
        }

    @Test
    fun `an archive of unknown length is cut off once it grows past what a day can hold`() =
        runTest {
            assertFailsWith<RadarDownloadException> {
                radarRemoteDataSource(responseBody = ByteReadChannel(content = ByteArray(size = ARCHIVE_ABOVE_LIMIT_BYTES))).getRadarArchive(
                    date = fifthOfJanuary,
                    archiveSink = archiveSink
                )
            }
            assertTrue(actual = archiveSink.size <= ARCHIVE_LIMIT_BYTES)
        }
}