package se.vadretapp.vadret.location

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.location.extension.toFailure
import se.vadretapp.vadret.location.extension.toLocation
import se.vadretapp.vadret.location.extension.toLocationPreference
import se.vadretapp.vadret.location.extension.toStoredLocation
import se.vadretapp.vadret.location.extension.toStoredLocationEntity
import se.vadretapp.vadret.location.local.DeviceLocationLocalDataSource
import se.vadretapp.vadret.location.local.LocationPreferenceEntity
import se.vadretapp.vadret.location.local.StoredLocationLocalDataSource
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation

internal class DefaultLocationRepository(
    private val deviceLocationLocalDataSource: DeviceLocationLocalDataSource,
    private val storedLocationLocalDataSource: StoredLocationLocalDataSource
) : LocationRepository {

    override suspend fun getDeviceLocation(): Either<Failure, Location> =
        either { deviceLocationLocalDataSource.getLastKnownLocation().toLocation() }
            .catch { exception: Exception -> exception.toFailure() }

    override suspend fun isAutomatic(): Either<Failure, Boolean> =
        either { storedLocationLocalDataSource.isAutomatic() }
            .catch { exception: Exception -> exception.toFailure() }

    override suspend fun setAutomatic(isAutomatic: Boolean): Either<Failure, Unit> =
        either { storedLocationLocalDataSource.setAutomatic(isAutomatic = isAutomatic) }
            .catch { exception: Exception -> exception.toFailure() }

    override suspend fun getStoredLocation(): Either<Failure, StoredLocation> =
        either { storedLocationLocalDataSource.getStoredLocation().toStoredLocation() }
            .catch { exception: Exception -> exception.toFailure() }

    override fun observeLocationPreference(): Flow<Either<Failure, LocationPreference>> =
        storedLocationLocalDataSource
            .observeLocationPreference()
            .map<LocationPreferenceEntity, Either<Failure, LocationPreference>> { locationPreferenceEntity: LocationPreferenceEntity ->
                locationPreferenceEntity.toLocationPreference().asRight()
            }.catch { throwable: Throwable ->
                if (throwable is Exception) {
                    emit(throwable.toFailure().asLeft())
                } else {
                    throw throwable
                }
            }

    override suspend fun selectStoredLocation(storedLocation: StoredLocation): Either<Failure, Unit> =
        either { storedLocationLocalDataSource.selectStoredLocation(storedLocationEntity = storedLocation.toStoredLocationEntity()) }
            .catch { exception: Exception -> exception.toFailure() }

    override suspend fun followFavorite(
        favoriteId: String,
        maximumFollowedFavorites: Int
    ): Either<Failure, Boolean> =
        either {
            storedLocationLocalDataSource.followFavorite(
                favoriteId = favoriteId,
                maximumFollowedFavorites = maximumFollowedFavorites
            )
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun unfollowFavorite(favoriteId: String): Either<Failure, Unit> =
        either { storedLocationLocalDataSource.unfollowFavorite(favoriteId = favoriteId) }
            .catch { exception: Exception -> exception.toFailure() }

    override suspend fun adoptFollowedFavoriteIds(favoriteIds: Set<String>): Either<Failure, Unit> =
        either { storedLocationLocalDataSource.adoptFollowedFavoriteIds(favoriteIds = favoriteIds) }
            .catch { exception: Exception -> exception.toFailure() }
}