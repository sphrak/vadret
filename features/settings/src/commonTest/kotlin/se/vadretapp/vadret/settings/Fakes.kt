package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import se.vadretapp.vadret.changelog.ChangelogRepository
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.developermode.DeveloperModeRepository
import se.vadretapp.vadret.di.model.AppInfo
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences
import se.vadretapp.vadret.library.LibraryRepository
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.place.PlaceRepository
import se.vadretapp.vadret.place.model.Place
import se.vadretapp.vadret.pollen.PollenRepository
import se.vadretapp.vadret.pollen.model.PollenForecast
import se.vadretapp.vadret.pollen.model.PollenRegion
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pollen.model.PollenTypes
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences
import se.vadretapp.vadret.pollenseason.ObservePollenInSeasonTask
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.setting.model.SystemSettings
import se.vadretapp.vadret.setting.model.Theme
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.time.Duration
import kotlin.time.Instant

internal val umea = Favorite(
    favoriteId = "favorite-umea",
    locality = "Umeå",
    municipality = "Umeå",
    county = "Västerbottens län",
    latitude = 63.8258,
    longitude = 20.263
)

internal val visby = Favorite(
    favoriteId = "favorite-visby",
    locality = "Visby",
    municipality = "Gotland",
    county = "Gotlands län",
    latitude = 57.634,
    longitude = 18.294
)

internal val kiruna = Place(
    locality = "Kiruna",
    municipality = "Kiruna",
    county = "Norrbottens län",
    latitude = 67.8558,
    longitude = 20.2253
)

internal val kalmar = Place(
    locality = "Kalmar",
    municipality = "Kalmar",
    county = "Kalmar län",
    latitude = 56.6634,
    longitude = 16.3566
)

internal fun Favorite.toStoredLocation(): StoredLocation =
    StoredLocation(
        locality = locality,
        municipality = municipality,
        county = county,
        latitude = latitude,
        longitude = longitude
    )

internal val developerAppInfo = AppInfo(
    applicationId = "se.vadretapp.vadret",
    versionName = "3.1.0-test",
    isDebug = true,
    isDeveloperBuild = true
)

internal val releaseAppInfo = developerAppInfo.copy(
    isDebug = false,
    isDeveloperBuild = false
)

internal class FakeSettingRepository(
    var theme: Either<Failure, Theme> = Theme.LIGHT.asRight(),
    var language: Either<Failure, AppLanguage> = AppLanguage.SYSTEM.asRight(),
    var hourFormatChoice: Either<Failure, HourFormatChoice> = HourFormatChoice.SYSTEM.asRight()
) : SettingRepository {
    var themeReads: Int = 0
    override suspend fun getTheme(): Either<Failure, Theme> {
        themeReads += 1
        return theme
    }
    override fun observeTheme(): Flow<Either<Failure, Theme>> = emptyFlow()
    override suspend fun setTheme(theme: Theme): Either<Failure, Unit> {
        this.theme = theme.asRight()
        return Unit.asRight()
    }
    override suspend fun getLanguage(): Either<Failure, AppLanguage> = language
    override fun observeLanguage(): Flow<Either<Failure, AppLanguage>> = emptyFlow()
    override suspend fun setLanguage(language: AppLanguage): Either<Failure, Unit> {
        this.language = language.asRight()
        return Unit.asRight()
    }
    override suspend fun getHourFormatChoice(): Either<Failure, HourFormatChoice> = hourFormatChoice
    override fun observeHourFormatChoice(): Flow<Either<Failure, HourFormatChoice>> = emptyFlow()
    override suspend fun setHourFormatChoice(hourFormatChoice: HourFormatChoice): Either<Failure, Unit> {
        this.hourFormatChoice = hourFormatChoice.asRight()
        return Unit.asRight()
    }
    val nativeNavigation: MutableStateFlow<Either<Failure, Boolean>> = MutableStateFlow(value = false.asRight())
    var nativeNavigationWriteFailure: Failure? = null
    override suspend fun isNativeNavigationEnabled(): Either<Failure, Boolean> = nativeNavigation.value
    override fun observeNativeNavigationEnabled(): Flow<Either<Failure, Boolean>> = nativeNavigation
    override suspend fun setNativeNavigationEnabled(isEnabled: Boolean): Either<Failure, Unit> {
        val writeFailure: Failure? = nativeNavigationWriteFailure
        if (writeFailure != null) {
            return writeFailure.asLeft()
        }
        nativeNavigation.value = isEnabled.asRight()
        return Unit.asRight()
    }
    override fun getSystemSettings(): Either<Failure, SystemSettings> = error("Settings only show the choices")
    override fun observeSystemSettings(): Flow<Either<Failure, SystemSettings>> = error("Settings only show the choices")
}

internal class FakeForecastPreferenceRepository(
    forecastPreferences: Either<Failure, ForecastPreferences> = ForecastPreferences(
        isHourlyForecastEnabled = false,
        isExpandOnceEnabled = true,
        isAuroraEnabled = false
    ).asRight()
) : ForecastPreferenceRepository {
    val forecastPreferences: MutableStateFlow<Either<Failure, ForecastPreferences>> = MutableStateFlow(value = forecastPreferences)
    var writeFailure: Failure? = null
    var hourlyForecastWriteDurations: Map<Boolean, Duration> = emptyMap()
    var observations = 0
    override suspend fun getForecastPreferences(): Either<Failure, ForecastPreferences> = forecastPreferences.value
    override fun observeForecastPreferences(): Flow<Either<Failure, ForecastPreferences>> {
        observations++
        return forecastPreferences
    }
    override suspend fun setHourlyForecastEnabled(isEnabled: Boolean): Either<Failure, Unit> {
        delay(duration = hourlyForecastWriteDurations[isEnabled] ?: Duration.ZERO)
        val currentWriteFailure: Failure? = writeFailure
        if (currentWriteFailure != null) {
            return currentWriteFailure.asLeft()
        }
        forecastPreferences.update { stored: Either<Failure, ForecastPreferences> ->
            stored.map { preferences: ForecastPreferences -> preferences.copy(isHourlyForecastEnabled = isEnabled) }
        }
        return Unit.asRight()
    }
    override suspend fun setExpandOnceEnabled(isEnabled: Boolean): Either<Failure, Unit> {
        forecastPreferences.update { stored: Either<Failure, ForecastPreferences> ->
            stored.map { preferences: ForecastPreferences -> preferences.copy(isExpandOnceEnabled = isEnabled) }
        }
        return Unit.asRight()
    }
    override suspend fun setAuroraEnabled(isEnabled: Boolean): Either<Failure, Unit> {
        val currentWriteFailure: Failure? = writeFailure
        if (currentWriteFailure != null) {
            return currentWriteFailure.asLeft()
        }
        forecastPreferences.update { stored: Either<Failure, ForecastPreferences> ->
            stored.map { preferences: ForecastPreferences -> preferences.copy(isAuroraEnabled = isEnabled) }
        }
        return Unit.asRight()
    }
}

internal class FakePollenPreferenceRepository(
    var pollenPreferences: Either<Failure, PollenPreferences> = PollenPreferences(
        isPollenEnabled = true,
        allergenPollenIds = emptySet(),
        isHibernationNoticeShown = true,
        isEndOfSeasonSimulated = false
    ).asRight()
) : PollenPreferenceRepository {
    var writeFailure: Failure? = null
    override suspend fun getPollenPreferences(): Either<Failure, PollenPreferences> = pollenPreferences
    override fun observePollenPreferences(): Flow<Either<Failure, PollenPreferences>> = error("Settings read the pollen preferences once")
    override suspend fun setPollenEnabled(isEnabled: Boolean): Either<Failure, Unit> {
        pollenPreferences = pollenPreferences.map { stored: PollenPreferences -> stored.copy(isPollenEnabled = isEnabled) }
        return Unit.asRight()
    }
    override suspend fun setAllergen(
        pollenId: String,
        isAllergen: Boolean
    ): Either<Failure, Unit> {
        val currentWriteFailure: Failure? = writeFailure
        if (currentWriteFailure != null) {
            return currentWriteFailure.asLeft()
        }
        pollenPreferences = pollenPreferences.map { stored: PollenPreferences ->
            stored.copy(
                allergenPollenIds = if (isAllergen) {
                    stored.allergenPollenIds + pollenId
                } else {
                    stored.allergenPollenIds - pollenId
                }
            )
        }
        return Unit.asRight()
    }
    override suspend fun setHibernationNoticeShown(isShown: Boolean): Either<Failure, Unit> {
        pollenPreferences = pollenPreferences.map { stored: PollenPreferences -> stored.copy(isHibernationNoticeShown = isShown) }
        return Unit.asRight()
    }
    override suspend fun setEndOfSeasonSimulated(isSimulated: Boolean): Either<Failure, Unit> {
        pollenPreferences = pollenPreferences.map { stored: PollenPreferences -> stored.copy(isEndOfSeasonSimulated = isSimulated) }
        return Unit.asRight()
    }
}

internal val hazelType = PollenType(
    pollenId = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323233",
    name = "Hassel",
    hasForecasts = true
)

internal val grassType = PollenType(
    pollenId = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323433",
    name = "Gräs",
    hasForecasts = true
)

internal val alternariaType = PollenType(
    pollenId = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323833",
    name = "Alternaria",
    hasForecasts = false
)

internal val birchType = PollenType(
    pollenId = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323332",
    name = "Björk",
    hasForecasts = true
)

internal val POLLEN_FETCHED_AT: Instant = Instant.parse(input = "2026-10-24T11:30:00Z")

internal class FakePollenRepository(
    var pollenTypes: Either<Failure, List<PollenType>> = listOf(hazelType, grassType, alternariaType, birchType).asRight()
) : PollenRepository {
    val savedPollenTypes: MutableStateFlow<PollenTypes?> = MutableStateFlow(value = null)

    override suspend fun getRegions(): Either<Failure, List<PollenRegion>> = error("Settings never ask for regions")
    override suspend fun getPollenTypes(): Either<Failure, List<PollenType>> = error("Settings observe the pollen types")
    override fun observePollenTypes(): Flow<Either<Failure, PollenTypes?>> =
        savedPollenTypes.map { saved: PollenTypes? -> saved.asRight() }
    override suspend fun refreshPollenTypes(): Either<Failure, PollenTypes> {
        val refreshed: Either<Failure, List<PollenType>> = pollenTypes
        if (refreshed is Either.Left) {
            return refreshed
        }
        val saved = PollenTypes(
            pollenTypes = (refreshed as Either.Right).b,
            fetchedAt = POLLEN_FETCHED_AT
        )
        savedPollenTypes.value = saved
        return saved.asRight()
    }
    override suspend fun getForecasts(
        startDate: LocalDate,
        endDate: LocalDate
    ): Either<Failure, List<PollenForecast>> = error("Settings never ask for forecasts")
    override suspend fun getCurrentForecasts(): Either<Failure, List<PollenForecast>> = error("Settings ask the pollen season task")
}

internal class FakeObservePollenInSeasonTask(
    var isInSeason: Either<Failure, Boolean> = true.asRight()
) : ObservePollenInSeasonTask {
    override fun invoke(): Flow<Either<Failure, Boolean>> = flowOf(isInSeason)
}

internal class FakeLibraryRepository(
    private val libraries: List<Library>
) : LibraryRepository {
    override fun getLibraries(): Either<Nothing, List<Library>> = libraries.asRight()
}

internal class FakeChangelogRepository(
    var changelog: Either<Failure, Changelog> = Changelog(markdown = "# Changelog").asRight()
) : ChangelogRepository {
    override suspend fun getChangelog(): Either<Failure, Changelog> = changelog
}

internal class FakeDeveloperModeRepository(
    isUnlocked: Boolean = false
) : DeveloperModeRepository {
    val isUnlocked: MutableStateFlow<Boolean> = MutableStateFlow(value = isUnlocked)
    var unlockResult: Either<Failure, Unit> = Unit.asRight()
    var unlocks: Int = 0

    override fun observeIsDeveloperModeUnlocked(): Flow<Either<Failure, Boolean>> = isUnlocked.map { unlocked: Boolean -> unlocked.asRight() }

    override suspend fun unlockDeveloperMode(): Either<Failure, Unit> {
        unlocks += 1
        if (unlockResult.isRight) {
            isUnlocked.value = true
        }
        return unlockResult
    }
}

internal class FakeLocationRepository(
    isAutomatic: Boolean = false
) : LocationRepository {

    val locationPreference: MutableStateFlow<LocationPreference> = MutableStateFlow(
        value = LocationPreference(
            isAutomatic = isAutomatic,
            storedLocation = umea.toStoredLocation()
        )
    )
    var writeFailure: Failure? = null

    override suspend fun getDeviceLocation(): Either<Failure, Location> =
        Location(
            latitude = 59.33,
            longitude = 18.07
        ).asRight()

    override suspend fun isAutomatic(): Either<Failure, Boolean> = locationPreference.value.isAutomatic.asRight()

    override suspend fun setAutomatic(isAutomatic: Boolean): Either<Failure, Unit> {
        val currentWriteFailure: Failure? = writeFailure
        if (currentWriteFailure != null) {
            return currentWriteFailure.asLeft()
        }
        locationPreference.update { preference: LocationPreference -> preference.copy(isAutomatic = isAutomatic) }
        return Unit.asRight()
    }

    override fun observeLocationPreference(): Flow<Either<Failure, LocationPreference>> =
        locationPreference.map { preference: LocationPreference -> preference.asRight() }

    override suspend fun selectStoredLocation(storedLocation: StoredLocation): Either<Failure, Unit> {
        val currentWriteFailure: Failure? = writeFailure
        if (currentWriteFailure != null) {
            return currentWriteFailure.asLeft()
        }
        locationPreference.value = LocationPreference(
            isAutomatic = false,
            storedLocation = storedLocation
        )
        return Unit.asRight()
    }
}

internal class FakeFavoriteRepository(
    initialFavorites: List<Favorite> = listOf(umea, visby)
) : FavoriteRepository {

    val favorites: MutableStateFlow<List<Favorite>> = MutableStateFlow(value = initialFavorites)
    var failure: Failure? = null
    var removeFailure: Failure? = null

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
        val currentRemoveFailure: Failure? = removeFailure
        if (currentRemoveFailure != null) {
            return currentRemoveFailure.asLeft()
        }
        favorites.update { favoriteList: List<Favorite> -> favoriteList.filterNot { favorite: Favorite -> favorite.favoriteId == favoriteId } }
        return Unit.asRight()
    }
}

internal class FakePlaceRepository(
    private val places: List<Place> = listOf(kiruna, kalmar)
) : PlaceRepository {
    val searches: MutableList<String> = mutableListOf()
    var failure: Failure? = null

    override suspend fun search(
        query: String,
        limit: Int
    ): Either<Failure, List<Place>> {
        searches.add(query)
        val currentFailure: Failure? = failure
        if (currentFailure != null) {
            return currentFailure.asLeft()
        }
        return places
            .filter { place: Place ->
                place.locality.startsWith(
                    prefix = query,
                    ignoreCase = true
                )
            }.asRight()
    }
}

internal fun library(
    author: String,
    title: String
): Library =
    Library(
        author = author,
        title = title,
        description = "$title description",
        projectUrl = "https://example.org/$title",
        sourceUrl = null,
        licenseUrl = "https://example.org/$title/license",
        license = "Apache 2.0"
    )

internal fun Place.toFavorite(favoriteId: String): Favorite =
    Favorite(
        favoriteId = favoriteId,
        locality = locality,
        municipality = municipality,
        county = county,
        latitude = latitude,
        longitude = longitude
    )