package se.vadretapp.vadret.wind.remote

import kotlin.time.Instant

internal interface WindRemoteDataSource {

    suspend fun getCreatedTime(): WindCreatedTimeDto

    suspend fun getWindParameter(
        validAt: Instant,
        windParameter: WindParameter,
        isGeometryIncluded: Boolean
    ): WindParameterDto
}