package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.successOrNull
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.FeedArea
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.GetChosenPlaceTask
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences
import se.vadretapp.vadret.icons.Overcast
import se.vadretapp.vadret.icons.VadretIcons
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.nominatim.NominatimRepository
import se.vadretapp.vadret.nominatim.model.Locality
import se.vadretapp.vadret.place.PlaceRepository
import se.vadretapp.vadret.place.model.Place
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.wsymb2_overcast
import se.vadretapp.vadret.warning.model.WarningPlace
import se.vadretapp.vadret.warning.usecase.ObserveUnseenFollowedPlaceWarningsTask
import se.vadretapp.vadret.warning.usecase.ObserveUnseenWarningsTask
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.ComingDay
import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.model.Forecasts
import se.vadretapp.vadret.weatherforecast.model.PlaceWeather
import se.vadretapp.vadret.weatherforecast.model.WeatherPlaceKind
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.time.Instant

internal val stockholm = StoredLocation(
    locality = "Stockholm",
    municipality = "Stockholm",
    county = "Stockholm",
    latitude = 59.3293,
    longitude = 18.0686
)

internal val kiruna = Place(
    locality = "Kiruna",
    municipality = "Kiruna",
    county = "Norrbotten",
    latitude = 67.8558,
    longitude = 20.2253
)

internal val kirunaStoredLocation = StoredLocation(
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

internal val noon = Forecast(
    validAt = Instant.parse(input = "2026-10-24T12:00:00Z"),
    intervalStartedAt = Instant.parse(input = "2026-10-24T11:00:00Z"),
    airTemperature = 4.0,
    windFromDirection = 270.0,
    windSpeed = 3.0,
    relativeHumidity = 70,
    precipitationType = 0,
    symbolCode = 1,
    precipitationAmount = 0.0
)

internal val morning = Analysis(
    analysedAt = Instant.parse(input = "2026-10-24T06:00:00Z"),
    airTemperature = 1.0,
    windFromDirection = 180.0,
    windSpeed = 2.0,
    relativeHumidity = 90,
    precipitationType = 0,
    symbolCode = 6,
    precipitationAmountLastHour = 0.0
)

internal fun comingDay(
    dayKey: String,
    isExpanded: Boolean = false
): ComingDay =
    ComingDay(
        key = dayKey,
        isToday = false,
        weekdayText = "Mon",
        dateText = "16 Nov",
        icon = VadretIcons.Weather.Overcast,
        descriptionResource = Res.string.wsymb2_overcast,
        precipitationText = null,
        lowTemperatureText = "1",
        highTemperatureText = "5",
        temperatureRangeStart = 0f,
        temperatureRangeEnd = 1f,
        hours = persistentListOf(),
        firstShownHourIndex = 0,
        isExpanded = isExpanded
    )

internal class FakeLocationRepository(
    isAutomatic: Either<Failure, Boolean> = false.asRight(),
    var deviceLocation: Either<Failure, Location> = Location(latitude = 57.7, longitude = 11.97).asRight(),
    var selectStoredLocationResult: Either<Failure, Unit> = Unit.asRight()
) : LocationRepository {

    val writes = mutableListOf<String>()

    val locationPreference: MutableStateFlow<LocationPreference> = MutableStateFlow(
        value = LocationPreference(
            isAutomatic = isAutomatic.successOrNull ?: false,
            storedLocation = stockholm
        )
    )

    var isAutomatic: Either<Failure, Boolean> = isAutomatic
        set(value) {
            field = value
            val automatic: Boolean? = value.successOrNull
            if (automatic != null) {
                locationPreference.update { preference: LocationPreference -> preference.copy(isAutomatic = automatic) }
            }
        }

    override suspend fun getDeviceLocation(): Either<Failure, Location> = deviceLocation

    override suspend fun isAutomatic(): Either<Failure, Boolean> = isAutomatic

    override suspend fun setAutomatic(isAutomatic: Boolean): Either<Failure, Unit> {
        writes.add("automatic=$isAutomatic")
        this.isAutomatic = isAutomatic.asRight()
        return Unit.asRight()
    }

    override fun observeLocationPreference(): Flow<Either<Failure, LocationPreference>> = locationPreference.map { preference: LocationPreference -> preference.asRight() }

    override suspend fun selectStoredLocation(storedLocation: StoredLocation): Either<Failure, Unit> {
        writes.add("selected=${storedLocation.locality}")
        if (selectStoredLocationResult.isRight) {
            this.isAutomatic = false.asRight()
            locationPreference.value = locationPreference.value.copy(
                isAutomatic = false,
                storedLocation = storedLocation
            )
        }
        return selectStoredLocationResult
    }
}

internal class FakeFavoriteRepository(
    var failure: Failure? = null
) : FavoriteRepository {

    val favorites: MutableStateFlow<List<Favorite>> = MutableStateFlow(value = emptyList())

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
        val currentFailure: Failure? = failure
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

internal val FAKE_FETCHED_AT: Instant = Instant.parse(input = "2026-10-24T11:30:00Z")

internal class FakeWeatherForecastRepository(
    var forecasts: Either<Failure, List<Forecast>> = listOf(noon).asRight(),
    var analyses: Either<Failure, List<Analysis>> = emptyList<Analysis>().asRight(),
    private val now: () -> Instant = { FAKE_FETCHED_AT }
) : WeatherForecastRepository {

    val requestedCoordinates = mutableListOf<Pair<Double, Double>>()
    val requestedWeatherPlaceKinds = mutableListOf<WeatherPlaceKind>()
    val forecastGates: MutableMap<Pair<Double, Double>, CompletableDeferred<Unit>> = mutableMapOf()
    var analysisGate: CompletableDeferred<Unit>? = null
    val savedWeather: MutableStateFlow<Map<Pair<Double, Double>, PlaceWeather>> = MutableStateFlow(value = emptyMap())

    override fun observeWeather(
        latitude: Double,
        longitude: Double
    ): Flow<Either<Failure, PlaceWeather?>> =
        savedWeather.map { saved: Map<Pair<Double, Double>, PlaceWeather> -> saved[latitude to longitude].asRight() }

    override suspend fun refreshForecasts(
        latitude: Double,
        longitude: Double,
        weatherPlaceKind: WeatherPlaceKind
    ): Either<Failure, Forecasts> {
        requestedCoordinates.add(latitude to longitude)
        requestedWeatherPlaceKinds.add(weatherPlaceKind)
        forecastGates[latitude to longitude]?.await()
        return when (val refreshedForecasts: Either<Failure, List<Forecast>> = forecasts) {
            is Either.Left -> refreshedForecasts
            is Either.Right -> {
                val fetchedAt: Instant = now()
                if (weatherPlaceKind != WeatherPlaceKind.DROPPED_PIN) {
                    savedWeather.update { saved: Map<Pair<Double, Double>, PlaceWeather> ->
                        saved + (
                            (latitude to longitude) to PlaceWeather(
                                forecasts = refreshedForecasts.b,
                                analyses = saved[latitude to longitude]?.analyses.orEmpty(),
                                fetchedAt = fetchedAt
                            )
                            )
                    }
                }
                Forecasts(
                    forecasts = refreshedForecasts.b,
                    fetchedAt = fetchedAt
                ).asRight()
            }
        }
    }

    override suspend fun refreshAnalyses(
        latitude: Double,
        longitude: Double,
        weatherPlaceKind: WeatherPlaceKind
    ): Either<Failure, List<Analysis>> {
        analysisGate?.await()
        val refreshedAnalyses: Either<Failure, List<Analysis>> = analyses
        if (refreshedAnalyses is Either.Right && weatherPlaceKind != WeatherPlaceKind.DROPPED_PIN) {
            savedWeather.update { saved: Map<Pair<Double, Double>, PlaceWeather> ->
                val savedPlace: PlaceWeather? = saved[latitude to longitude]
                if (savedPlace == null) {
                    saved
                } else {
                    saved + ((latitude to longitude) to savedPlace.copy(analyses = refreshedAnalyses.b))
                }
            }
        }
        return refreshedAnalyses
    }
}

internal class FakeNominatimRepository(
    var locality: Either<Failure, Locality> = Locality(
        name = "Göteborg",
        municipality = "Göteborgs kommun",
        county = "Västra Götalands län"
    ).asRight()
) : NominatimRepository {
    override suspend fun getLocality(
        latitude: Double,
        longitude: Double
    ): Either<Failure, Locality> = locality
}

internal class FakePlaceRepository(
    var items: List<Place> = listOf(kiruna)
) : PlaceRepository {

    val searches = mutableListOf<Pair<String, Int>>()

    override suspend fun search(
        query: String,
        limit: Int
    ): Either<Failure, List<Place>> {
        searches.add(query to limit)
        return items
            .filter { place: Place ->
                place.locality.startsWith(
                    prefix = query,
                    ignoreCase = true
                )
            }.asRight()
    }
}

internal fun warning(
    identifier: String,
    endsAt: Instant? = null
): AggregatedFeed =
    AggregatedFeed(
        identifier = identifier,
        headline = "Vind",
        areaName = "Stockholm",
        warningLevel = WarningLevel.YELLOW,
        details = emptyList(),
        source = FeedSource.SMHI,
        isPublicAlert = false,
        senderName = null,
        publishedAt = Instant.parse(input = "2026-10-24T08:00:00Z"),
        startsAt = null,
        endsAt = endsAt,
        area = FeedArea(
            polygons = emptyList(),
            lines = emptyList(),
            regions = emptyList()
        ),
        slipperiness = null
    )

// Answers for one place and for the followed places alike, and records which was asked: no place means the followed places.
internal class FakeObserveUnseenWarningsTask(
    initial: Either<Failure, List<AggregatedFeed>> = listOf(
        warning(identifier = "smhi-1"),
        warning(identifier = "smhi-2")
    ).asRight()
) : ObserveUnseenWarningsTask,
    ObserveUnseenFollowedPlaceWarningsTask {

    val unseenWarnings: MutableStateFlow<Either<Failure, List<AggregatedFeed>>> = MutableStateFlow(value = initial)
    val requests = mutableListOf<Pair<WarningPlace?, Boolean>>()

    override fun invoke(
        warningPlace: WarningPlace,
        isRefresh: Boolean
    ): Flow<Either<Failure, List<AggregatedFeed>>> {
        requests.add(element = warningPlace to isRefresh)
        return unseenWarnings
    }

    override fun invoke(isRefresh: Boolean): Flow<Either<Failure, List<AggregatedFeed>>> {
        requests.add(element = null to isRefresh)
        return unseenWarnings
    }
}

internal class FakeForecastPreferenceRepository : ForecastPreferenceRepository {

    private var forecastPreferences = ForecastPreferences(
        isHourlyForecastEnabled = false,
        isExpandOnceEnabled = true
    )
    private val observedForecastPreferences = MutableStateFlow<Either<Failure, ForecastPreferences>>(value = forecastPreferences.asRight())

    override suspend fun getForecastPreferences(): Either<Failure, ForecastPreferences> = forecastPreferences.asRight()

    override fun observeForecastPreferences(): Flow<Either<Failure, ForecastPreferences>> = observedForecastPreferences

    override suspend fun setHourlyForecastEnabled(isEnabled: Boolean): Either<Failure, Unit> {
        store(forecastPreferences = forecastPreferences.copy(isHourlyForecastEnabled = isEnabled))
        return Unit.asRight()
    }

    override suspend fun setExpandOnceEnabled(isEnabled: Boolean): Either<Failure, Unit> {
        store(forecastPreferences = forecastPreferences.copy(isExpandOnceEnabled = isEnabled))
        return Unit.asRight()
    }

    private fun store(forecastPreferences: ForecastPreferences) {
        this.forecastPreferences = forecastPreferences
        observedForecastPreferences.value = forecastPreferences.asRight()
    }
}

// Reads the location fake the way the chosen place is found, so the forecast tests keep steering it through that fake.
internal class FakeGetChosenPlaceTask(
    private val locationRepository: FakeLocationRepository,
    private val nominatimRepository: FakeNominatimRepository
) : GetChosenPlaceTask {

    override suspend fun invoke(): Either<Failure, FollowedPlace> =
        locationRepository.isAutomatic().flatMap { isAutomatic: Boolean ->
            if (isAutomatic) {
                locationRepository.getDeviceLocation().flatMap { location: Location ->
                    val locality: Locality? = nominatimRepository
                        .getLocality(
                            latitude = location.latitude,
                            longitude = location.longitude
                        ).successOrNull
                    FollowedPlace(
                        favoriteId = null,
                        name = locality?.name,
                        municipality = locality?.municipality,
                        county = locality?.county,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        isChosen = true
                    ).asRight()
                }
            } else {
                locationRepository.observeLocationPreference().first().map { preference: LocationPreference -> preference.storedLocation }.flatMap { storedLocation: StoredLocation ->
                    FollowedPlace(
                        favoriteId = null,
                        name = storedLocation.locality,
                        municipality = storedLocation.municipality,
                        county = storedLocation.county,
                        latitude = storedLocation.latitude,
                        longitude = storedLocation.longitude,
                        isChosen = true
                    ).asRight()
                }
            }
        }
}