package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.extension.toStoredLocation
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.nominatim.NominatimRepository
import se.vadretapp.vadret.nominatim.model.Locality
import se.vadretapp.vadret.place.PlaceRepository
import se.vadretapp.vadret.place.model.Place
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.fetchAndIncrement

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

internal val kiruna = Place(
    locality = "Kiruna",
    municipality = "Kiruna",
    county = "Norrbotten",
    latitude = 67.8558,
    longitude = 20.2253
)

internal val kirunaFavorite = Favorite(
    favoriteId = "favorite-1",
    locality = "Kiruna",
    municipality = "Kiruna",
    county = "Norrbotten",
    latitude = 67.8558,
    longitude = 20.2253
)

internal class FakeLocationRepository(
    isAutomatic: Boolean = true,
    var deviceLocation: Either<Failure, Location> = Location(
        latitude = 57.7089,
        longitude = 11.9746
    ).asRight(),
    var selectStoredLocationResult: Either<Failure, Unit> = Unit.asRight()
) : LocationRepository {

    val locationPreference: MutableStateFlow<LocationPreference> = MutableStateFlow(
        value = LocationPreference(
            isAutomatic = isAutomatic,
            storedLocation = umea.toStoredLocation()
        )
    )
    val writes: MutableList<String> = mutableListOf()
    var failure: Failure? = null

    override suspend fun getDeviceLocation(): Either<Failure, Location> = deviceLocation

    override suspend fun isAutomatic(): Either<Failure, Boolean> = failure?.asLeft() ?: locationPreference.value.isAutomatic.asRight()

    override suspend fun setAutomatic(isAutomatic: Boolean): Either<Failure, Unit> {
        writes.add("automatic=$isAutomatic")
        locationPreference.update { preference: LocationPreference -> preference.copy(isAutomatic = isAutomatic) }
        return Unit.asRight()
    }

    override fun observeLocationPreference(): Flow<Either<Failure, LocationPreference>> =
        locationPreference.map { preference: LocationPreference ->
            val currentFailure: Failure? = failure
            if (currentFailure != null) {
                currentFailure.asLeft()
            } else {
                preference.asRight()
            }
        }

    override suspend fun selectStoredLocation(storedLocation: StoredLocation): Either<Failure, Unit> {
        writes.add("selected=${storedLocation.locality}")
        if (selectStoredLocationResult.isRight) {
            locationPreference.value = LocationPreference(
                isAutomatic = false,
                storedLocation = storedLocation
            )
        }
        return selectStoredLocationResult
    }
}

internal class FakeFavoriteRepository(
    initialFavorites: List<Favorite> = listOf(umea, visby, cabin)
) : FavoriteRepository {

    val favorites: MutableStateFlow<List<Favorite>> = MutableStateFlow(value = initialFavorites)
    val maximumFavoritesAsked: MutableList<Int> = mutableListOf()
    var failure: Failure? = null
    var addFailure: Failure? = null

    private val nextFavoriteNumber: AtomicInt = AtomicInt(value = 1)

    override fun observeFavorites(): Flow<Either<Failure, List<Favorite>>> =
        favorites.map { favoriteList: List<Favorite> ->
            val currentFailure: Failure? = failure
            if (currentFailure != null) {
                currentFailure.asLeft()
            } else {
                favoriteList.asRight()
            }
        }

    override suspend fun addFavorite(
        locality: String,
        municipality: String,
        county: String,
        latitude: Double,
        longitude: Double,
        maximumFavorites: Int
    ): Either<Failure, Favorite> {
        maximumFavoritesAsked.add(maximumFavorites)
        val currentFailure: Failure? = failure ?: addFailure
        if (currentFailure != null) {
            return currentFailure.asLeft()
        }
        val storedFavorite: Favorite? = favorites.value.firstOrNull { favorite: Favorite ->
            favorite.locality == locality && favorite.municipality == municipality && favorite.county == county
        }
        if (storedFavorite != null) {
            return storedFavorite.asRight()
        }
        if (favorites.value.size >= maximumFavorites) {
            return Failure(errorCode = ErrorCode.FOLLOWED_PLACE_LIMIT).asLeft()
        }
        val favorite = Favorite(
            favoriteId = "favorite-${nextFavoriteNumber.fetchAndIncrement()}",
            locality = locality,
            municipality = municipality,
            county = county,
            latitude = latitude,
            longitude = longitude
        )
        favorites.update { favoriteList: List<Favorite> -> favoriteList + favorite }
        return favorite.asRight()
    }

    override suspend fun removeFavorite(favoriteId: String): Either<Failure, Unit> {
        val currentFailure: Failure? = failure
        if (currentFailure != null) {
            return currentFailure.asLeft()
        }
        favorites.update { favoriteList: List<Favorite> -> favoriteList.filterNot { favorite: Favorite -> favorite.favoriteId == favoriteId } }
        return Unit.asRight()
    }
}

internal class FakePlaceRepository(
    var items: List<Place> = listOf(kiruna)
) : PlaceRepository {

    val searches: MutableList<Pair<String, Int>> = mutableListOf()

    override suspend fun search(
        prefix: String,
        limit: Int
    ): Either<Failure, List<Place>> {
        searches.add(prefix to limit)
        return items
            .filter { place: Place ->
                place.locality.startsWith(
                    prefix = prefix,
                    ignoreCase = true
                )
            }.asRight()
    }
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