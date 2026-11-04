package se.vadretapp.vadret.radar.local

import kotlinx.datetime.LocalDate
import okio.BufferedSink

internal interface RadarImageLocalDataSource {

    suspend fun getImage(
        date: LocalDate,
        imageName: String
    ): ByteArray?
    suspend fun getImageNames(date: LocalDate): Set<String>
    suspend fun setImage(
        date: LocalDate,
        imageName: String,
        image: ByteArray
    )
    suspend fun setArchive(
        date: LocalDate,
        writeArchive: suspend (BufferedSink) -> Unit
    )
}