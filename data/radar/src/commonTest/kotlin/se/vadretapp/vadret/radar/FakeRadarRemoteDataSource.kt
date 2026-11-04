package se.vadretapp.vadret.radar

import kotlinx.coroutines.yield
import kotlinx.datetime.LocalDate
import okio.BufferedSink
import se.vadretapp.vadret.radar.exception.RadarDownloadException
import se.vadretapp.vadret.radar.remote.RadarDto
import se.vadretapp.vadret.radar.remote.RadarRemoteDataSource

internal class FakeRadarRemoteDataSource(
    var radarDto: RadarDto
) : RadarRemoteDataSource {

    val archives = mutableMapOf<LocalDate, ByteArray>()
    val images = mutableMapOf<String, ByteArray>()
    val requestedDates = mutableListOf<LocalDate>()
    val downloads = mutableListOf<String>()

    override suspend fun getRadar(date: LocalDate): RadarDto {
        requestedDates.add(date)
        return radarDto
    }

    // Yields so that images asked for at the same time meet while the archive is on its way.
    override suspend fun getRadarArchive(
        date: LocalDate,
        archiveSink: BufferedSink
    ) {
        downloads.add("archive $date")
        yield()
        archiveSink.write(source = archives[date] ?: throw RadarDownloadException(message = "No archive for $date"))
    }

    override suspend fun getRadarImage(
        date: LocalDate,
        imageName: String
    ): ByteArray {
        downloads.add("image $date $imageName")
        return images[imageName] ?: throw RadarDownloadException(message = "No image $imageName")
    }
}