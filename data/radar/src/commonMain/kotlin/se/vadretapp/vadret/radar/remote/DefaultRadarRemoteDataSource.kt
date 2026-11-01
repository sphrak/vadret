package se.vadretapp.vadret.radar.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

private const val IMAGE_FORMAT = "tif"
private const val TIME_ZONE = "GMT"

internal class DefaultRadarRemoteDataSource(
    private val httpClient: Lazy<HttpClient>
) : RadarRemoteDataSource {

    override suspend fun getRadar(date: LocalDate): RadarDto =
        httpClient.value.get(urlString = "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/${date.year}/${date.month.number}/${date.day}") {
            expectSuccess = true
            parameter(key = "format", value = IMAGE_FORMAT)
            parameter(key = "timeZone", value = TIME_ZONE)
        }.body()
}