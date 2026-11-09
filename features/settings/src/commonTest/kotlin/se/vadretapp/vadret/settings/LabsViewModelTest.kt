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
import se.vadretapp.vadret.settings.model.Labs
import se.vadretapp.vadret.settings.usecase.DefaultObserveLabsTask
import se.vadretapp.vadret.settings.usecase.DefaultObserveNativeNavigationTask
import se.vadretapp.vadret.settings.usecase.DefaultSetAuroraTask
import se.vadretapp.vadret.settings.usecase.DefaultSetHourlyForecastTask
import se.vadretapp.vadret.settings.usecase.DefaultSetNativeNavigationTask
import se.vadretapp.vadret.settings.usecase.ObserveNativeNavigationTask
import se.vadretapp.vadret.settings.usecase.UnavailableObserveNativeNavigationTask
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
    private val settingRepository = FakeSettingRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        observeNativeNavigationTask: ObserveNativeNavigationTask = DefaultObserveNativeNavigationTask(settingRepository = settingRepository)
    ): LabsViewModel {
        val labsViewModel = LabsViewModel(
            observeLabsTask = DefaultObserveLabsTask(forecastPreferenceRepository = forecastPreferenceRepository),
            setHourlyForecastTask = DefaultSetHourlyForecastTask(forecastPreferenceRepository = forecastPreferenceRepository),
            setAuroraTask = DefaultSetAuroraTask(forecastPreferenceRepository = forecastPreferenceRepository),
            observeNativeNavigationTask = observeNativeNavigationTask,
            setNativeNavigationTask = DefaultSetNativeNavigationTask(settingRepository = settingRepository),
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
            val shown = mutableListOf<Boolean?>(labsViewModel.state.value.labs?.isHourlyForecastEnabled)

            listOf(true, false).forEach { isHourlyForecastEnabled: Boolean ->
                labsViewModel.onEvent(event = LabsEvent.OnHourlyForecastToggled(isHourlyForecastEnabled = isHourlyForecastEnabled))
                advanceUntilIdle()
                shown += labsViewModel.state.value.labs?.isHourlyForecastEnabled
            }

            assertEquals(expected = listOf<Boolean?>(false, true, false), actual = shown)
        }

    @Test
    fun `aurora is off until turned on and each toggle is stored and shown without touching the hourly forecast`() =
        testScope.runTest {
            val labsViewModel: LabsViewModel = viewModel()
            advanceUntilIdle()
            val shown = mutableListOf<Labs?>(labsViewModel.state.value.labs)

            listOf(true, false).forEach { isAuroraEnabled: Boolean ->
                labsViewModel.onEvent(event = LabsEvent.OnAuroraToggled(isAuroraEnabled = isAuroraEnabled))
                advanceUntilIdle()
                shown += labsViewModel.state.value.labs
            }

            assertEquals(
                expected = listOf<Labs?>(
                    Labs(isHourlyForecastEnabled = false, isAuroraEnabled = false),
                    Labs(isHourlyForecastEnabled = false, isAuroraEnabled = true),
                    Labs(isHourlyForecastEnabled = false, isAuroraEnabled = false)
                ),
                actual = shown
            )
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
                actual = forecastPreferenceRepository.forecastPreferences.value.successOrNull?.isHourlyForecastEnabled to labsViewModel.state.value.labs?.isHourlyForecastEnabled
            )
        }

    @Test
    fun `an hourly forecast setting that cannot be read shows no switch for it but says why`() =
        testScope.runTest {
            forecastPreferenceRepository.forecastPreferences.value = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft()

            val labsViewModel: LabsViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                expected = LabsState(
                    isNativeNavigationEnabled = false,
                    failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
                ),
                actual = labsViewModel.state.value
            )
        }

    @Test
    fun `turning native navigation on and off is stored and shown`() =
        testScope.runTest {
            val labsViewModel: LabsViewModel = viewModel()
            advanceUntilIdle()
            val shown = mutableListOf<Boolean?>(labsViewModel.state.value.isNativeNavigationEnabled)

            listOf(true, false).forEach { isNativeNavigationEnabled: Boolean ->
                labsViewModel.onEvent(event = LabsEvent.OnNativeNavigationToggled(isNativeNavigationEnabled = isNativeNavigationEnabled))
                advanceUntilIdle()
                shown += labsViewModel.state.value.isNativeNavigationEnabled
            }

            assertEquals(expected = listOf<Boolean?>(false, true, false), actual = shown)
            assertEquals(expected = false, actual = settingRepository.nativeNavigation.value.successOrNull)
        }

    @Test
    fun `a platform without native navigation shows no switch for it`() =
        testScope.runTest {
            val labsViewModel: LabsViewModel = viewModel(observeNativeNavigationTask = UnavailableObserveNativeNavigationTask())
            advanceUntilIdle()

            assertEquals(
                expected = LabsState(labs = Labs(isHourlyForecastEnabled = false, isAuroraEnabled = false)),
                actual = labsViewModel.state.value
            )
        }

    @Test
    fun `a native navigation choice that cannot be read shows no switch for it but says why`() =
        testScope.runTest {
            settingRepository.nativeNavigation.value = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft()

            val labsViewModel: LabsViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                expected = LabsState(
                    labs = Labs(isHourlyForecastEnabled = false, isAuroraEnabled = false),
                    failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
                ),
                actual = labsViewModel.state.value
            )
        }

    @Test
    fun `a native navigation toggle that cannot be stored keeps the switch as it was and says why`() =
        testScope.runTest {
            settingRepository.nativeNavigationWriteFailure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            val labsViewModel: LabsViewModel = viewModel()
            advanceUntilIdle()

            labsViewModel.onEvent(event = LabsEvent.OnNativeNavigationToggled(isNativeNavigationEnabled = true))
            advanceUntilIdle()

            assertEquals(
                expected = LabsState(
                    labs = Labs(isHourlyForecastEnabled = false, isAuroraEnabled = false),
                    isNativeNavigationEnabled = false,
                    failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
                ),
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
            val shownForHourlyForecast: LabsState = labsViewModel.state.value
            labsViewModel.onEvent(event = LabsEvent.OnFailureDismissed)
            labsViewModel.onEvent(event = LabsEvent.OnAuroraToggled(isAuroraEnabled = true))
            advanceUntilIdle()
            val shownForAurora: LabsState = labsViewModel.state.value
            labsViewModel.onEvent(event = LabsEvent.OnFailureDismissed)
            advanceUntilIdle()

            val storedLabs = Labs(
                isHourlyForecastEnabled = false,
                isAuroraEnabled = false
            )
            assertEquals(
                expected = listOf(
                    LabsState(
                        labs = storedLabs,
                        isNativeNavigationEnabled = false,
                        failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
                    ),
                    LabsState(
                        labs = storedLabs,
                        isNativeNavigationEnabled = false,
                        failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
                    ),
                    LabsState(
                        labs = storedLabs,
                        isNativeNavigationEnabled = false
                    )
                ),
                actual = listOf(
                    shownForHourlyForecast,
                    shownForAurora,
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