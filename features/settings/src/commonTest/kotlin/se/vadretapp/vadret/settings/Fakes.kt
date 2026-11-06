package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
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
import se.vadretapp.vadret.language.LanguageRepository
import se.vadretapp.vadret.language.model.AppLanguage
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
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences
import se.vadretapp.vadret.pollenseason.ObservePollenInSeasonTask
import se.vadretapp.vadret.theme.ThemeRepository
import se.vadretapp.vadret.theme.model.Theme
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.fetchAndIncrement

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

internal class FakeLanguageRepository(
    var language: Either<Failure, AppLanguage> = AppLanguage.SYSTEM.asRight()
) : LanguageRepository {
    override suspend fun getLanguage(): Either<Failure, AppLanguage> = language
    override fun observeLanguage(): Flow<Either<Failure, AppLanguage>> = emptyFlow()
    override suspend fun setLanguage(language: AppLanguage): Either<Failure, Unit> {
        this.language = language.asRight()
        return Unit.asRight()
    }
}

internal class FakeThemeRepository(
    var theme: Either<Failure, Theme> = Theme.LIGHT.asRight()
) : ThemeRepository {
    var reads: Int = 0
    override suspend fun getTheme(): Either<Failure, Theme> {
        reads += 1
        return theme
    }
    override fun observeTheme(): Flow<Either<Failure, Theme>> = emptyFlow()
    override suspend fun setTheme(theme: Theme): Either<Failure, Unit> {
        this.theme = theme.asRight()
        return Unit.asRight()
    }
}

internal class FakeForecastPreferenceRepository(
    var forecastPreferences: Either<Failure, ForecastPreferences> = ForecastPreferences(
        isHourlyForecastEnabled = false,
        isExpandOnceEnabled = true
    ).asRight()
) : ForecastPreferenceRepository {
    override suspend fun getForecastPreferences(): Either<Failure, ForecastPreferences> = forecastPreferences
    override fun observeForecastPreferences(): Flow<Either<Failure, ForecastPreferences>> = emptyFlow()
    override suspend fun setHourlyForecastEnabled(isEnabled: Boolean): Either<Failure, Unit> {
        forecastPreferences = forecastPreferences.map { stored: ForecastPreferences -> stored.copy(isHourlyForecastEnabled = isEnabled) }
        return Unit.asRight()
    }
    override suspend fun setExpandOnceEnabled(isEnabled: Boolean): Either<Failure, Unit> {
        forecastPreferences = forecastPreferences.map { stored: ForecastPreferences -> stored.copy(isExpandOnceEnabled = isEnabled) }
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

internal class FakePollenRepository(
    var pollenTypes: Either<Failure, List<PollenType>> = listOf(hazelType, grassType, alternariaType, birchType).asRight(),
    var fetchedPollenTypes: Either<Failure, List<PollenType>>? = null
) : PollenRepository {
    override suspend fun getRegions(): Either<Failure, List<PollenRegion>> = error("Settings never ask for regions")
    override suspend fun getPollenTypes(): Either<Failure, List<PollenType>> = error("Settings observe the pollen types")
    override fun observePollenTypes(): Flow<Either<Failure, List<PollenType>>> = flowOf(pollenTypes, fetchedPollenTypes).filterNotNull()
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
        prefix: String,
        limit: Int
    ): Either<Failure, List<Place>> {
        searches.add(prefix)
        val currentFailure: Failure? = failure
        if (currentFailure != null) {
            return currentFailure.asLeft()
        }
        return places
            .filter { place: Place ->
                place.locality.startsWith(
                    prefix = prefix,
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