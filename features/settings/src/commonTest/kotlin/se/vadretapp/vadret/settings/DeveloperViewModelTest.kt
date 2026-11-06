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
import se.vadretapp.vadret.settings.usecase.DefaultGetSettingsTask
import se.vadretapp.vadret.settings.usecase.DefaultSetEndOfSeasonSimulatedTask
import se.vadretapp.vadret.settings.usecase.DefaultSetExpandOnceTask
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DeveloperViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(context = testDispatcher)
    private val forecastPreferenceRepository = FakeForecastPreferenceRepository()
    private val pollenPreferenceRepository = FakePollenPreferenceRepository()
    private val themeRepository = FakeThemeRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): DeveloperViewModel {
        val developerViewModel = DeveloperViewModel(
            getSettingsTask = DefaultGetSettingsTask(
                themeRepository = themeRepository,
                languageRepository = FakeLanguageRepository(),
                forecastPreferenceRepository = forecastPreferenceRepository,
                pollenPreferenceRepository = pollenPreferenceRepository
            ),
            setExpandOnceTask = DefaultSetExpandOnceTask(forecastPreferenceRepository = forecastPreferenceRepository),
            setEndOfSeasonSimulatedTask = DefaultSetEndOfSeasonSimulatedTask(pollenPreferenceRepository = pollenPreferenceRepository),
            coroutineDispatcher = testDispatcher
        )
        developerViewModel.onEvent(event = DeveloperEvent.OnStart)
        return developerViewModel
    }

    @Test
    fun `turning expand once off is stored and shown`() =
        testScope.runTest {
            val developerViewModel: DeveloperViewModel = viewModel()
            advanceUntilIdle()

            developerViewModel.onEvent(event = DeveloperEvent.OnExpandOnceToggled(isExpandOnceEnabled = false))
            advanceUntilIdle()

            assertEquals(expected = false, actual = developerViewModel.state.value.settings?.isExpandOnceEnabled)
        }

    @Test
    fun `simulating the end of the season is stored and shown`() =
        testScope.runTest {
            val developerViewModel: DeveloperViewModel = viewModel()
            advanceUntilIdle()

            developerViewModel.onEvent(event = DeveloperEvent.OnEndOfSeasonSimulationToggled(isEndOfSeasonSimulated = true))
            advanceUntilIdle()

            assertEquals(expected = true, actual = developerViewModel.state.value.settings?.isEndOfSeasonSimulated)
        }

    @Test
    fun `settings that cannot be read show nothing`() =
        testScope.runTest {
            themeRepository.theme = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft()

            val developerViewModel: DeveloperViewModel = viewModel()
            advanceUntilIdle()

            assertNull(actual = developerViewModel.state.value.settings)
        }

    @Test
    fun `starting again reads the settings once`() =
        testScope.runTest {
            val developerViewModel: DeveloperViewModel = viewModel()
            developerViewModel.onEvent(event = DeveloperEvent.OnStart)
            advanceUntilIdle()

            assertEquals(expected = 1, actual = themeRepository.reads)
        }
}