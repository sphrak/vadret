package se.vadretapp.vadret.radar.remote

import kotlinx.datetime.LocalDate
import okio.BufferedSink

internal interface RadarRemoteDataSource {

    suspend fun getRadar(date: LocalDate): RadarDto
    suspend fun getRadarArchive(
        date: LocalDate,
        archiveSink: BufferedSink
    )
    suspend fun getRadarImage(
        date: LocalDate,
        imageName: String
    ): ByteArray
}