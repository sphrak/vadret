package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation

internal class FakeLocationRepository(
    private val isAutomatic: Boolean = true,
    private val deviceLocation: Either<Failure, Location> = Location(latitude = 59.3293, longitude = 18.0686).asRight(),
    private val storedLocation: StoredLocation = MALMO,
    private val followedFavoriteIds: Set<String> = EVERY_FAVORITE_ID,
    private val failure: Failure? = null
) : LocationRepository {

    val followed = mutableListOf<String>()

    override suspend fun getDeviceLocation(): Either<Failure, Location> = deviceLocation

    override suspend fun isAutomatic(): Either<Failure, Boolean> = isAutomatic.asRight()

    override suspend fun setAutomatic(isAutomatic: Boolean): Either<Failure, Unit> = Unit.asRight()

    override fun observeLocationPreference(): Flow<Either<Failure, LocationPreference>> =
        flowOf(
            failure?.asLeft() ?: LocationPreference(
                isAutomatic = isAutomatic,
                storedLocation = storedLocation,
                followedFavoriteIds = followedFavoriteIds
            ).asRight()
        )

    override suspend fun selectStoredLocation(storedLocation: StoredLocation): Either<Failure, Unit> = Unit.asRight()

    override suspend fun setFavoriteFollowed(
        favoriteId: String,
        isFollowed: Boolean
    ): Either<Failure, Unit> {
        if (isFollowed) {
            followed += favoriteId
        }
        return Unit.asRight()
    }
}

private val MALMO = StoredLocation(
    locality = "Malmö",
    municipality = "Malmö",
    county = "Skåne län",
    latitude = 55.6050,
    longitude = 13.0038
)