package se.vadretapp.vadret.aggregatedfeed.remote

internal interface KrisinformationRemoteDataSource {
    suspend fun getNews(): List<KrisinformationFeedDto>
    suspend fun getPublicAlerts(): List<KrisinformationFeedDto>
}