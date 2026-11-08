package se.vadretapp.vadret.settings

import io.github.sphrak.either.extension.asLeft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.di.model.AppInfo
import se.vadretapp.vadret.followedplace.ObserveLocationChoicesTask
import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.settings.model.RowSummary
import se.vadretapp.vadret.settings.usecase.DefaultGetSettingsTask
import se.vadretapp.vadret.settings.usecase.DefaultObserveDeveloperModeTask
import se.vadretapp.vadret.settings.usecase.DefaultSetLanguageTask
import se.vadretapp.vadret.settings.usecase.DefaultSetThemeTask
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.followed_place_position
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(context = testDispatcher)
    private val themeRepository = FakeThemeRepository()
    private val languageRepository = FakeLanguageRepository()
    private val forecastPreferenceRepository = FakeForecastPreferenceRepository()
    private val pollenPreferenceRepository = FakePollenPreferenceRepository()
    private val locationRepository = FakeLocationRepository()
    private val favoriteRepository = FakeFavoriteRepository()
    private val developerModeRepository = FakeDeveloperModeRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(appInfo: AppInfo = releaseAppInfo): SettingsViewModel {
        val settingsViewModel = SettingsViewModel(
            getSettingsTask = DefaultGetSettingsTask(
                themeRepository = themeRepository,
                languageRepository = languageRepository,
                forecastPreferenceRepository = forecastPreferenceRepository,
                pollenPreferenceRepository = pollenPreferenceRepository
            ),
            setThemeTask = DefaultSetThemeTask(themeRepository = themeRepository),
            setLanguageTask = DefaultSetLanguageTask(languageRepository = languageRepository),
            observeLocationChoicesTask = ObserveLocationChoicesTask(
                favoriteRepository = favoriteRepository,
                locationRepository = locationRepository
            ),
            observeDeveloperModeTask = DefaultObserveDeveloperModeTask(
                developerModeRepository = developerModeRepository,
                appInfo = appInfo
            ),
            coroutineDispatcher = testDispatcher
        )
        settingsViewModel.onEvent(event = SettingsEvent.OnStart)
        return settingsViewModel
    }

    @Test
    fun `the stored settings are shown`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                expected = Settings(
                    theme = Theme.LIGHT,
                    language = AppLanguage.SYSTEM,
                    isHourlyForecastEnabled = false,
                    isExpandOnceEnabled = true,
                    isPollenEnabled = true,
                    allergenPollenIds = emptySet(),
                    isEndOfSeasonSimulated = false
                ),
                actual = settingsViewModel.state.value.settings
            )
        }

    @Test
    fun `settings that cannot be read are not shown`() =
        testScope.runTest {
            themeRepository.theme = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft()

            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            assertNull(actual = settingsViewModel.state.value.settings)
        }

    @Test
    fun `the places row names the chosen place until the position is followed`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()
            val chosen: RowSummary? = settingsViewModel.state.value.placesSummary

            locationRepository.setAutomatic(isAutomatic = true)
            advanceUntilIdle()

            assertEquals(
                expected = listOf(
                    RowSummary.Plain(text = "Umeå"),
                    RowSummary.Resource(resource = Res.string.followed_place_position)
                ),
                actual = listOf(chosen, settingsViewModel.state.value.placesSummary)
            )
        }

    @Test
    fun `a chosen place that is no saved place is still named`() =
        testScope.runTest {
            favoriteRepository.favorites.value = emptyList()

            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(expected = RowSummary.Plain(text = "Umeå"), actual = settingsViewModel.state.value.placesSummary)
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

            assertEquals(
                expected = Triple(
                    first = true,
                    second = false,
                    third = Theme.AMOLED
                ),
                actual = Triple(
                    first = isOpened,
                    second = settingsViewModel.state.value.isThemeDialogVisible,
                    third = settingsViewModel.state.value.settings?.theme
                )
            )
        }

    @Test
    fun `dismissing the theme dialog keeps the theme`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            settingsViewModel.onEvent(event = SettingsEvent.OnThemeRowClicked)
            settingsViewModel.onEvent(event = SettingsEvent.OnThemeDialogDismissed)
            advanceUntilIdle()

            assertEquals(
                expected = false to Theme.LIGHT,
                actual = settingsViewModel.state.value.isThemeDialogVisible to settingsViewModel.state.value.settings?.theme
            )
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

            assertEquals(
                expected = Triple(
                    first = true,
                    second = false,
                    third = AppLanguage.SWEDISH
                ),
                actual = Triple(
                    first = isOpened,
                    second = settingsViewModel.state.value.isLanguageDialogVisible,
                    third = settingsViewModel.state.value.settings?.language
                )
            )
        }

    @Test
    fun `dismissing the language dialog keeps the language`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()

            settingsViewModel.onEvent(event = SettingsEvent.OnLanguageRowClicked)
            settingsViewModel.onEvent(event = SettingsEvent.OnLanguageDialogDismissed)
            advanceUntilIdle()

            assertEquals(
                expected = false to AppLanguage.SYSTEM,
                actual = settingsViewModel.state.value.isLanguageDialogVisible to settingsViewModel.state.value.settings?.language
            )
        }

    @Test
    fun `the developer row shows in a developer build`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel(appInfo = developerAppInfo)
            advanceUntilIdle()

            assertEquals(expected = true, actual = settingsViewModel.state.value.isDeveloperRowVisible)
        }

    @Test
    fun `the developer row shows in another build once developer mode is unlocked`() =
        testScope.runTest {
            val settingsViewModel: SettingsViewModel = viewModel()
            advanceUntilIdle()
            val isVisibleBefore: Boolean = settingsViewModel.state.value.isDeveloperRowVisible

            developerModeRepository.unlockDeveloperMode()
            advanceUntilIdle()

            assertEquals(
                expected = false to true,
                actual = isVisibleBefore to settingsViewModel.state.value.isDeveloperRowVisible
            )
        }
}