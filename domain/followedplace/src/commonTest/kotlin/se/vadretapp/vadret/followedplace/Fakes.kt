package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.nominatim.NominatimRepository
import se.vadretapp.vadret.nominatim.model.Locality

internal val umea = Favorite(
    favoriteId = "favorite-umea",
    locality = "Umeå",
    municipality = "Umeå",
    county = "Västerbotten",
    latitude = 63.8258,
    longitude = 20.263
)

internal val visby = Favorite(
    favoriteId = "favorite-visby",
    locality = "Visby",
    municipality = "Gotland",
    county = "Gotland",
    latitude = 57.634,
    longitude = 18.294
)

internal val cabin = Favorite(
    favoriteId = "favorite-cabin",
    locality = "Stugan",
    municipality = "",
    county = "",
    latitude = 61.8286,
    longitude = 16.0912
)

internal fun Favorite.toStoredLocation(): StoredLocation =
    StoredLocation(
        locality = locality,
        municipality = municipality,
        county = county,
        latitude = latitude,
        longitude = longitude
    )

internal class FakeLocationRepository(
    var deviceLocation: Either<Failure, Location> = Location(
        latitude = 57.7089,
        longitude = 11.9746
    ).asRight()
) : LocationRepository {

    val locationPreference: MutableStateFlow<LocationPreference> = MutableStateFlow(
        value = LocationPreference(
            isAutomatic = true,
            storedLocation = umea.toStoredLocation(),
            followedFavoriteIds = emptySet()
        )
    )
    var failure: Failure? = null

    override suspend fun getDeviceLocation(): Either<Failure, Location> = deviceLocation

    override suspend fun isAutomatic(): Either<Failure, Boolean> = error("Followed places read the observed preference")

    override suspend fun setAutomatic(isAutomatic: Boolean): Either<Failure, Unit> = error("Followed places never change the location mode")

    override suspend fun getStoredLocation(): Either<Failure, StoredLocation> = error("Followed places read the observed preference")

    override fun observeLocationPreference(): Flow<Either<Failure, LocationPreference>> =
        locationPreference.map { preference: LocationPreference ->
            val currentFailure: Failure? = failure
            if (currentFailure != null) {
                currentFailure.asLeft()
            } else {
                preference.asRight()
            }
        }

    override suspend fun selectStoredLocation(storedLocation: StoredLocation): Either<Failure, Unit> = error("Followed places never choose a place")

    override suspend fun followFavorite(
        favoriteId: String,
        maximumFollowedFavorites: Int
    ): Either<Failure, Boolean> {
        val followedFavoriteIds: Set<String> = locationPreference.value.followedFavoriteIds
        val isFollowed: Boolean = favoriteId in followedFavoriteIds || followedFavoriteIds.size < maximumFollowedFavorites
        if (isFollowed) {
            locationPreference.update { preference: LocationPreference -> preference.copy(followedFavoriteIds = preference.followedFavoriteIds + favoriteId) }
        }
        return isFollowed.asRight()
    }

    override suspend fun unfollowFavorite(favoriteId: String): Either<Failure, Unit> {
        locationPreference.update { preference: LocationPreference -> preference.copy(followedFavoriteIds = preference.followedFavoriteIds - favoriteId) }
        return Unit.asRight()
    }

    override suspend fun adoptFollowedFavoriteIds(favoriteIds: Set<String>): Either<Failure, Unit> = error("Followed places never adopt push favorites")
}

internal class FakeFavoriteRepository : FavoriteRepository {

    val favorites: MutableStateFlow<List<Favorite>> = MutableStateFlow(value = listOf(umea, visby, cabin))

    override fun observeFavorites(): Flow<Either<Failure, List<Favorite>>> = favorites.map { favoriteList: List<Favorite> -> favoriteList.asRight() }

    override suspend fun addFavorite(
        locality: String,
        municipality: String,
        county: String,
        latitude: Double,
        longitude: Double
    ): Either<Failure, Favorite> = error("Followed places never add a favorite")

    override suspend fun removeFavorite(favoriteId: String): Either<Failure, Unit> = error("Followed places never remove a favorite")
}

internal class FakeNominatimRepository(
    var locality: Either<Failure, Locality> = Locality(
        name = "Göteborg",
        municipality = "Göteborg",
        county = "Västra Götaland"
    ).asRight()
) : NominatimRepository {

    override suspend fun getLocality(
        latitude: Double,
        longitude: Double
    ): Either<Failure, Locality> = locality
}