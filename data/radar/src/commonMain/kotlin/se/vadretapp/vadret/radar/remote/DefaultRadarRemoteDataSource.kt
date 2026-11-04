package se.vadretapp.vadret.radar.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readRemaining
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.io.readByteArray
import okio.BufferedSink
import se.vadretapp.vadret.radar.exception.RadarDownloadException

private const val IMAGE_FORMAT = "tif"
private const val TIME_ZONE = "GMT"
private const val NO_STORE = "no-store"
private const val MAXIMUM_ARCHIVE_BYTES: Long = 67_108_864L
private const val MAXIMUM_IMAGE_BYTES: Long = 1_048_576L
private const val COPY_BUFFER_BYTES = 65_536
private const val END_OF_BODY = -1

internal class DefaultRadarRemoteDataSource(
    private val httpClient: Lazy<HttpClient>
) : RadarRemoteDataSource {

    override suspend fun getRadar(date: LocalDate): RadarDto =
        httpClient.value.get(urlString = "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/${date.year}/${date.month.number}/${date.day}") {
            expectSuccess = true
            parameter(key = "format", value = IMAGE_FORMAT)
            parameter(key = "timeZone", value = TIME_ZONE)
        }.body()

    // SMHI answers 406 to an archive or image asked for only as JSON, which the client otherwise asks for by default.
    // The images are kept on disk once downloaded, so the HTTP cache would only hold a second copy.
    override suspend fun getRadarArchive(
        date: LocalDate,
        archiveSink: BufferedSink
    ) {
        httpClient.value.prepareGet(urlString = "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/${date.year}/${date.month.number}/${date.day}.zip") {
            expectSuccess = true
            parameter(key = "format", value = IMAGE_FORMAT)
            parameter(key = "timeZone", value = TIME_ZONE)
            accept(contentType = ContentType.Any)
            header(key = HttpHeaders.CacheControl, value = NO_STORE)
        }.execute { httpResponse: HttpResponse ->
            httpResponse.copyBody(
                sink = archiveSink,
                maximumBytes = MAXIMUM_ARCHIVE_BYTES
            )
        }
    }

    override suspend fun getRadarImage(
        date: LocalDate,
        imageName: String
    ): ByteArray =
        httpClient.value.prepareGet(urlString = "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/${date.year}/${date.month.number}/${date.day}/$imageName") {
            expectSuccess = true
            accept(contentType = ContentType.Any)
            header(key = HttpHeaders.CacheControl, value = NO_STORE)
        }.execute { httpResponse: HttpResponse -> httpResponse.readBytes(maximumBytes = MAXIMUM_IMAGE_BYTES) }

    // The archive goes straight to disk, so a whole day never has to fit in memory at once.
    private suspend fun HttpResponse.copyBody(
        sink: BufferedSink,
        maximumBytes: Long
    ) {
        val declaredBytes: Long? = contentLength()
        if (declaredBytes != null && declaredBytes > maximumBytes) {
            throw RadarDownloadException(message = "${request.url} declares $declaredBytes bytes, more than $maximumBytes")
        }
        val body: ByteReadChannel = bodyAsChannel()
        val buffer: ByteArray = ByteArray(size = COPY_BUFFER_BYTES)
        var copiedBytes: Long = 0L
        var readBytes: Int = body.readAvailable(buffer = buffer)
        while (readBytes != END_OF_BODY) {
            copiedBytes += readBytes
            if (copiedBytes > maximumBytes) {
                throw RadarDownloadException(message = "${request.url} is larger than $maximumBytes bytes")
            }
            sink.write(
                source = buffer,
                offset = 0,
                byteCount = readBytes
            )
            readBytes = body.readAvailable(buffer = buffer)
        }
    }

    private suspend fun HttpResponse.readBytes(maximumBytes: Long): ByteArray {
        val bytes: ByteArray = bodyAsChannel().readRemaining(max = maximumBytes + 1).readByteArray()
        if (bytes.size > maximumBytes) {
            throw RadarDownloadException(message = "${request.url} is larger than $maximumBytes bytes")
        }
        return bytes
    }
}