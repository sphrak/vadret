package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.onResult
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.nominatim.NominatimRepository
import se.vadretapp.vadret.nominatim.model.Locality
import se.vadretapp.vadret.warning.extension.affects
import se.vadretapp.vadret.warning.model.WarningPlace

internal class DefaultObserveChosenPlaceWarningsTask(
    private val feedRepository: FeedRepository,
    private val locationRepository: LocationRepository,
    private val nominatimRepository: NominatimRepository
) : ObserveChosenPlaceWarningsTask {

    // The warnings follow the place the weather is shown for, so choosing another place shows its warnings.
    override fun invoke(isRefresh: Boolean): Flow<Either<Failure, List<AggregatedFeed>>> =
        locationRepository
            .observeLocationPreference()
            .distinctUntilChanged()
            .map { locationPreference: Either<Failure, LocationPreference> ->
                locationPreference
                    .flatMap { preference: LocationPreference -> getWarningPlace(locationPreference = preference) }
                    .flatMap { warningPlace: WarningPlace ->
                        getAffectingWarnings(
                            warningPlace = warningPlace,
                            isRefresh = isRefresh
                        )
                    }
            }

    private suspend fun getWarningPlace(locationPreference: LocationPreference): Either<Failure, WarningPlace> =
        if (locationPreference.isAutomatic) {
            getDeviceWarningPlace()
        } else {
            locationPreference.storedLocation.toWarningPlace().asRight()
        }

    // A device location Nominatim cannot name is still matched by its coordinates.
    private suspend fun getDeviceWarningPlace(): Either<Failure, WarningPlace> =
        locationRepository
            .getDeviceLocation()
            .flatMap { location: Location ->
                val locality: Locality? = nominatimRepository
                    .getLocality(
                        latitude = location.latitude,
                        longitude = location.longitude
                    ).onResult(
                        onError = { _: Failure -> null },
                        onSuccess = { locality: Locality -> locality }
                    )
                WarningPlace(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    county = locality?.county,
                    municipality = locality?.municipality
                ).asRight()
            }

    private suspend fun getAffectingWarnings(
        warningPlace: WarningPlace,
        isRefresh: Boolean
    ): Either<Failure, List<AggregatedFeed>> =
        feedRepository
            .getAggregatedFeed(isRefresh = isRefresh)
            .map { aggregatedFeeds: List<AggregatedFeed> ->
                aggregatedFeeds.filter { aggregatedFeed: AggregatedFeed -> aggregatedFeed.affects(warningPlace = warningPlace) }
            }

    private fun StoredLocation.toWarningPlace(): WarningPlace =
        WarningPlace(
            latitude = latitude,
            longitude = longitude,
            county = county,
            municipality = municipality
        )
}