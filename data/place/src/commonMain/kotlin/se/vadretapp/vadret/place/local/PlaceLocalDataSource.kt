package se.vadretapp.vadret.place.local

internal interface PlaceLocalDataSource {
    suspend fun search(
        prefix: String,
        limit: Int
    ): List<PlaceEntity>
}