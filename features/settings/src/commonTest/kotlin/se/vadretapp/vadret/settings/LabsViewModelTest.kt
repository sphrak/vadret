package se.vadretapp.vadret.settings

import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.successOrNull
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
import se.vadretapp.vadret.settings.usecase.DefaultObserveHourlyForecastTask
import se.vadretapp.vadret.settings.usecase.DefaultSetHourlyForecastTask
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class LabsViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(context = testDispatcher)
    private val forecastPreferenceRepository = FakeForecastPreferenceRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): LabsViewModel {
        val labsViewModel = LabsViewModel(
            observeHourlyForecastTask = DefaultObserveHourlyForecastTask(forecastPreferenceRepository = forecastPreferenceRepository),
            setHourlyForecastTask = DefaultSetHourlyForecastTask(forecastPreferenceRepository = forecastPreferenceRepository),
            coroutineDispatcher = testDispatcher
        )
        labsViewModel.onEvent(event = LabsEvent.OnStart)
        return labsViewModel
    }

    @Test
    fun `turning the hourly forecast on and off is stored and shown`() =
        testScope.runTest {
            val labsViewModel: LabsViewModel = viewModel()
            advanceUntilIdle()
            val shown = mutableListOf<Boolean?>(labsViewModel.state.value.isHourlyForecastEnabled)

            listOf(true, false).forEach { isHourlyForecastEnabled: Boolean ->
                labsViewModel.onEvent(event = LabsEvent.OnHourlyForecastToggled(isHourlyForecastEnabled = isHourlyForecastEnabled))
                advanceUntilIdle()
                shown += labsViewModel.state.value.isHourlyForecastEnabled
            }

            assertEquals(expected = listOf<Boolean?>(false, true, false), actual = shown)
        }

    @Test
    fun `the last of quick toggles is what is stored and shown even when an earlier one is slower to store`() =
        testScope.runTest {
            forecastPreferenceRepository.hourlyForecastWriteDurations = mapOf(
                true to 2.seconds,
                false to 1.seconds
            )
            val labsViewModel: LabsViewModel = viewModel()
            advanceUntilIdle()

            labsViewModel.onEvent(event = LabsEvent.OnHourlyForecastToggled(isHourlyForecastEnabled = true))
            labsViewModel.onEvent(event = LabsEvent.OnHourlyForecastToggled(isHourlyForecastEnabled = false))
            advanceUntilIdle()

            assertEquals(
                expected = false to false,
                actual = forecastPreferenceRepository.forecastPreferences.value.successOrNull?.isHourlyForecastEnabled to labsViewModel.state.value.isHourlyForecastEnabled
            )
        }

    @Test
    fun `a setting that cannot be read shows no switch but says why`() =
        testScope.runTest {
            forecastPreferenceRepository.forecastPreferences.value = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft()

            val labsViewModel: LabsViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                expected = LabsState(failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)),
                actual = labsViewModel.state.value
            )
        }

    @Test
    fun `a toggle that cannot be stored keeps the switch as it was and says why until dismissed`() =
        testScope.runTest {
            forecastPreferenceRepository.writeFailure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            val labsViewModel: LabsViewModel = viewModel()
            advanceUntilIdle()

            labsViewModel.onEvent(event = LabsEvent.OnHourlyForecastToggled(isHourlyForecastEnabled = true))
            advanceUntilIdle()
            val shown: LabsState = labsViewModel.state.value
            labsViewModel.onEvent(event = LabsEvent.OnFailureDismissed)
            advanceUntilIdle()

            assertEquals(
                expected = listOf(
                    LabsState(
                        isHourlyForecastEnabled = false,
                        failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
                    ),
                    LabsState(isHourlyForecastEnabled = false)
                ),
                actual = listOf(
                    shown,
                    labsViewModel.state.value
                )
            )
        }

    @Test
    fun `starting again follows the setting once`() =
        testScope.runTest {
            val labsViewModel: LabsViewModel = viewModel()
            labsViewModel.onEvent(event = LabsEvent.OnStart)
            advanceUntilIdle()

            assertEquals(expected = 1, actual = forecastPreferenceRepository.observations)
        }
}