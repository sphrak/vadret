package se.vadretapp.vadret.wind.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val DOWNSAMPLE = 10

private val validTimeFormat = LocalDateTime.Format {
    year()
    monthNumber()
    day()
    char(value = 'T')
    hour()
    minute()
    second()
    char(value = 'Z')
}

internal class DefaultWindRemoteDataSource(
    private val httpClient: Lazy<HttpClient>
) : WindRemoteDataSource {

    override suspend fun getCreatedTime(): WindCreatedTimeDto =
        httpClient.value.get(urlString = "https://opendata-download-metfcst.smhi.se/api/category/snow1g/version/1/createdtime.json") {
            expectSuccess = true
        }.body()

    override suspend fun getWindParameter(
        validAt: Instant,
        windParameter: WindParameter,
        isGeometryIncluded: Boolean
    ): WindParameterDto {
        val validTime: String = validAt
            .toLocalDateTime(timeZone = TimeZone.UTC)
            .format(format = validTimeFormat)
        return httpClient.value.get(
            urlString = "https://opendata-download-metfcst.smhi.se/api/category/snow1g/version/1/geotype/multipoint/time/$validTime/parameter/${windParameter.parameterName}/data.json"
        ) {
            expectSuccess = true
            parameter(key = "downsample", value = DOWNSAMPLE)
            parameter(key = "with-geo", value = isGeometryIncluded)
        }.body()
    }
}