package se.vadretapp.vadret.aggregatedfeed.remote

internal interface SmhiWarningRemoteDataSource {
    suspend fun getWarnings(): List<SmhiWarningDto>
}