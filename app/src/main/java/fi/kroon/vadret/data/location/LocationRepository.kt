package fi.kroon.vadret.data.location

import android.util.Log
import fi.kroon.vadret.data.exception.Either
import fi.kroon.vadret.data.exception.Failure
import fi.kroon.vadret.data.location.exception.LocationFailure
import fi.kroon.vadret.data.location.model.Location
import io.reactivex.Single
import javax.inject.Inject

const val LORP = "LocationRepository"

class LocationRepository @Inject constructor(
    private val locationProvider: LocationProvider
) {

    fun get(): Single<Either<Failure, Location>> {
        return Single.just(
            locationProvider.get()
        ).doOnEvent {
            t1, t2 -> Log.d(LORP, "T1: $t1, T2: $t2")
        }.doOnError {
            Log.d(LORP, "$it")
        }.onErrorReturn {
            Either.Left(LocationFailure.LocationNotAvailableFailure())
        }
    }
}