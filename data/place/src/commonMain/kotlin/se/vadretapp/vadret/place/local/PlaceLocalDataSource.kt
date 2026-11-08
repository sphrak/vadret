package se.vadretapp.vadret.place.local

internal interface PlaceLocalDataSource {
    suspend fun search(
        query: String,
        limit: Int
    ): List<PlaceEntity>
}