package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.map
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import se.vadretapp.vadret.changelog.ChangelogRepository
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.setting_city
import se.vadretapp.vadret.core.resources.setting_county
import se.vadretapp.vadret.core.resources.setting_latitude
import se.vadretapp.vadret.core.resources.setting_longitude
import se.vadretapp.vadret.core.resources.setting_municipality
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
import se.vadretapp.vadret.model.AppInfo
import se.vadretapp.vadret.settings.model.AboutRowHint
import se.vadretapp.vadret.settings.model.LibraryGroup
import se.vadretapp.vadret.settings.model.LocationRow
import se.vadretapp.vadret.settings.ui.SettingsPage
import se.vadretapp.vadret.theme.ThemeRepository
import se.vadretapp.vadret.theme.model.Theme
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val stockholm = StoredLocation(
    locality = "Stockholm",
    municipality = "Stockholm",
    county = "Stockholm",
    latitude = 59.3293,
    longitude = 18.0686
)

private class FakeLanguageRepository(
    var language: Either<Failure, AppLanguage> = AppLanguage.SYSTEM.asRight()
) : LanguageRepository {
    override suspend fun getLanguage(): Either<Failure, AppLanguage> = language
    override fun observeLanguage(): Flow<Either<Failure, AppLanguage>> = emptyFlow()
    override suspend fun setLanguage(language: AppLanguage): Either<Failure, Unit> {
        this.language = language.asRight()
        return Unit.asRight()
    }
}

private class FakeThemeRepository(
    var theme: Either<Failure, Theme> = Theme.LIGHT.asRight()
) : ThemeRepository {
    override suspend fun getTheme(): Either<Failure, Theme> = theme
    override fun observeTheme(): Flow<Either<Failure, Theme>> = emptyFlow()
    override suspend fun setTheme(theme: Theme): Either<Failure, Unit> {
        this.theme = theme.asRight()
        return Unit.asRight()
    }
}

private class FakeLocationRepository(
    var isAutomatic: Either<Failure, Boolean> = true.asRight()
) : LocationRepository {
    override suspend fun getDeviceLocation(): Either<Failure, Location> = error("Settings never ask for the device location")
    override suspend fun isAutomatic(): Either<Failure, Boolean> = isAutomatic
    override suspend fun setAutomatic(isAutomatic: Boolean): Either<Failure, Unit> {
        this.isAutomatic = isAutomatic.asRight()
        return Unit.asRight()
    }
    override suspend fun getStoredLocation(): Either<Failure, StoredLocation> = stockholm.asRight()
    override fun observeLocationPreference(): Flow<Either<Failure, LocationPreference>> = error("Settings never observe the location")
    override suspend fun selectStoredLocation(storedLocation: StoredLocation): Either<Failure, Unit> = error("Settings never store a place")
}

private class FakeForecastPreferenceRepository(
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

private class FakeLibraryRepository : LibraryRepository {
    override fun getLibraries(): Either<Nothing, List<Library>> = listOf(ktor, coil, kotlinxDatetime).asRight()
}

private class FakeChangelogRepository(
    var changelog: Either<Failure, Changelog> = Changelog(markdown = "# Changelog").asRight()
) : ChangelogRepository {
    override suspend fun getChangelog(): Either<Failure, Changelog> = changelog
}

private fun library(
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

private val ktor: Library = library(author = "JetBrains", title = "Ktor")
private val coil: Library = library(author = "Coil Contributors", title = "Coil")
private val kotlinxDatetime: Library = library(author = "JetBrains", title = "kotlinx-datetime")

private val debugAppInfo = AppInfo(
    applicationId = "se.vadretapp.vadret",
    versionName = "3.1.0-test",
    isDebug = true,
    isDeveloperBuild = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val themeRepository = FakeThemeRepository()
    private val languageRepository = FakeLanguageRepository()
    private val locationRepository = FakeLocationRepository()
    private val changelogRepository = FakeChangelogRepository()
    private val forecastPreferenceRepository = FakeForecastPreferenceRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(appInfo: AppInfo = debugAppInfo): SettingsViewModel {
        val settingsViewModel = SettingsViewModel(
            getSettingsTask = DefaultGetSettingsTask(
                locationRepository = locationRepository,
                themeRepository = themeRepository,
                languageRepository = languageRepository,
                forecastPreferenceRepository = forecastPreferenceRepository
            ),
            setLocationModeTask = DefaultSetLocationModeTask(locationRepository = locationRepository),
            setThemeTask = DefaultSetThemeTask(themeRepository = themeRepository),
            setLanguageTask = DefaultSetLanguageTask(languageRepository = languageRepository),
            setHourlyForecastTask = DefaultSetHourlyForecastTask(forecastPreferenceRepository = forecastPreferenceRepository),
            setExpandOnceTask = DefaultSetExpandOnceTask(forecastPreferenceRepository = forecastPreferenceRepository),
            getLibrariesTask = DefaultGetLibrariesTask(libraryRepository = FakeLibraryRepository()),
            getChangelogTask = DefaultGetChangelogTask(changelogRepository = changelogRepository),
            appInfo = appInfo,
            coroutineDispatcher = testDispatcher
        )
        settingsViewModel.onEvent(event = SettingsEvent.OnStart)
        return settingsViewModel
    }

    @Test
    fun `settings and libraries are loaded`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            val state: SettingsState = settingsViewModel.state.value
            assertEquals(
                expected = Settings(
                    isAutomaticLocation = true,
                    theme = Theme.LIGHT,
                    language = AppLanguage.SYSTEM,
                    isHourlyForecastEnabled = false,
                    isExpandOnceEnabled = true,
                    storedLocation = stockholm
                ),
                actual = state.settings
            )
            assertEquals(
                expected = listOf(
                    LibraryGroup(author = "JetBrains", libraries = persistentListOf(ktor, kotlinxDatetime)),
                    LibraryGroup(author = "Coil Contributors", libraries = persistentListOf(coil))
                ),
                actual = state.libraryGroups
            )
            assertEquals(
                expected = listOf(
                    LocationRow(titleResource = Res.string.setting_city, value = stockholm.locality),
                    LocationRow(titleResource = Res.string.setting_municipality, value = stockholm.municipality),
                    LocationRow(titleResource = Res.string.setting_county, value = stockholm.county),
                    LocationRow(titleResource = Res.string.setting_latitude, value = stockholm.latitude.toString()),
                    LocationRow(titleResource = Res.string.setting_longitude, value = stockholm.longitude.toString())
                ),
                actual = state.locationRows
            )
            assertEquals(expected = 4, actual = state.aboutRows.size)
            assertEquals(
                expected = AboutRowHint.Plain(text = debugAppInfo.versionName),
                actual = state.aboutRows.first().hint
            )
        }

    @Test
    fun `settings that cannot be read are not shown`() =
        testScope.runTest {
            themeRepository.theme = Failure(errorCode = ErrorCode.UNKNOWN).asLeft()

            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            assertNull(actual = settingsViewModel.state.value.settings)
        }

    @Test
    fun `a language that cannot be read hides the settings`() =
        testScope.runTest {
            languageRepository.language = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft()

            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            assertNull(actual = settingsViewModel.state.value.settings)
        }

    @Test
    fun `turning the automatic location off is stored and shown`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            settingsViewModel.onEvent(event = SettingsEvent.OnLocationModeToggled(isAutomaticLocation = false))
            advanceUntilIdle()

            assertEquals(expected = false, actual = settingsViewModel.state.value.settings?.isAutomaticLocation)
        }

    @Test
    fun `turning the hourly forecast on is stored and shown`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            settingsViewModel.onEvent(event = SettingsEvent.OnHourlyForecastToggled(isHourlyForecastEnabled = true))
            advanceUntilIdle()

            assertEquals(expected = true, actual = settingsViewModel.state.value.settings?.isHourlyForecastEnabled)
        }

    @Test
    fun `turning expand once off is stored and shown`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            settingsViewModel.onEvent(event = SettingsEvent.OnExpandOnceToggled(isExpandOnceEnabled = false))
            advanceUntilIdle()

            assertEquals(
                expected = ForecastPreferences(
                    isHourlyForecastEnabled = false,
                    isExpandOnceEnabled = false
                ).asRight(),
                actual = forecastPreferenceRepository.forecastPreferences
            )
            assertEquals(expected = false, actual = settingsViewModel.state.value.settings?.isExpandOnceEnabled)
        }

    @Test
    fun `the developer tab is shown only in developer builds`() =
        testScope.runTest {
            assertEquals(
                expected = listOf(
                    listOf(SettingsPage.PREFERENCES, SettingsPage.LIBRARIES, SettingsPage.ABOUT, SettingsPage.DEVELOPER),
                    listOf(SettingsPage.PREFERENCES, SettingsPage.LIBRARIES, SettingsPage.ABOUT)
                ),
                actual = listOf(
                    viewModel(appInfo = debugAppInfo).state.value.pages,
                    viewModel(appInfo = debugAppInfo.copy(isDeveloperBuild = false)).state.value.pages
                )
            )
        }

    @Test
    fun `forecast preferences that cannot be read hide the settings`() =
        testScope.runTest {
            forecastPreferenceRepository.forecastPreferences = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft()

            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            assertNull(actual = settingsViewModel.state.value.settings)
        }

    @Test
    fun `choosing a theme closes the dialog and shows the theme`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            settingsViewModel.onEvent(event = SettingsEvent.OnThemeRowClicked)
            val isOpened: Boolean = settingsViewModel.state.value.isThemeDialogVisible
            settingsViewModel.onEvent(event = SettingsEvent.OnThemeSelected(theme = Theme.AMOLED))
            advanceUntilIdle()

            assertEquals(expected = true, actual = isOpened)
            assertEquals(expected = false, actual = settingsViewModel.state.value.isThemeDialogVisible)
            assertEquals(expected = Theme.AMOLED, actual = settingsViewModel.state.value.settings?.theme)
        }

    @Test
    fun `dismissing the theme dialog keeps the theme`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            settingsViewModel.onEvent(event = SettingsEvent.OnThemeRowClicked)
            settingsViewModel.onEvent(event = SettingsEvent.OnThemeDialogDismissed)
            advanceUntilIdle()

            assertEquals(expected = false, actual = settingsViewModel.state.value.isThemeDialogVisible)
            assertEquals(expected = Theme.LIGHT, actual = settingsViewModel.state.value.settings?.theme)
        }

    @Test
    fun `choosing a language closes the dialog and shows the language`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            settingsViewModel.onEvent(event = SettingsEvent.OnLanguageRowClicked)
            val isOpened: Boolean = settingsViewModel.state.value.isLanguageDialogVisible
            settingsViewModel.onEvent(event = SettingsEvent.OnLanguageSelected(language = AppLanguage.SWEDISH))
            advanceUntilIdle()

            assertEquals(expected = true, actual = isOpened)
            assertEquals(expected = false, actual = settingsViewModel.state.value.isLanguageDialogVisible)
            assertEquals(expected = AppLanguage.SWEDISH, actual = settingsViewModel.state.value.settings?.language)
            assertEquals(expected = AppLanguage.SWEDISH.asRight(), actual = languageRepository.language)
        }

    @Test
    fun `dismissing the language dialog keeps the language`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            settingsViewModel.onEvent(event = SettingsEvent.OnLanguageRowClicked)
            settingsViewModel.onEvent(event = SettingsEvent.OnLanguageDialogDismissed)
            advanceUntilIdle()

            assertEquals(expected = false, actual = settingsViewModel.state.value.isLanguageDialogVisible)
            assertEquals(expected = AppLanguage.SYSTEM, actual = settingsViewModel.state.value.settings?.language)
        }

    @Test
    fun `a clicked library is shown until it is dismissed`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            settingsViewModel.onEvent(event = SettingsEvent.OnLibraryClicked(library = coil))
            val shown: Library? = settingsViewModel.state.value.selectedLibrary
            settingsViewModel.onEvent(event = SettingsEvent.OnLibraryDismissed)

            assertEquals(expected = coil, actual = shown)
            assertNull(actual = settingsViewModel.state.value.selectedLibrary)
        }

    @Test
    fun `the changelog is shown until it is dismissed`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            settingsViewModel.onEvent(event = SettingsEvent.OnChangelogClicked)
            val isShown: Boolean = settingsViewModel.state.value.isChangelogVisible
            settingsViewModel.onEvent(event = SettingsEvent.OnChangelogDismissed)

            assertEquals(expected = Changelog(markdown = "# Changelog"), actual = settingsViewModel.state.value.changelog)
            assertEquals(expected = true, actual = isShown)
            assertEquals(expected = false, actual = settingsViewModel.state.value.isChangelogVisible)
        }

    @Test
    fun `a changelog that cannot be read is never shown`() =
        testScope.runTest {
            changelogRepository.changelog = Failure(errorCode = ErrorCode.FILE_READ_FAILED).asLeft()
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            settingsViewModel.onEvent(event = SettingsEvent.OnChangelogClicked)

            assertNull(actual = settingsViewModel.state.value.changelog)
            assertEquals(expected = false, actual = settingsViewModel.state.value.isChangelogVisible)
        }
}