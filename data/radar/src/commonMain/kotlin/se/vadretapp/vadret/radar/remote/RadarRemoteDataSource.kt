package se.vadretapp.vadret.radar.remote

import kotlinx.datetime.LocalDate

internal interface RadarRemoteDataSource {

    suspend fun getRadar(date: LocalDate): RadarDto
}