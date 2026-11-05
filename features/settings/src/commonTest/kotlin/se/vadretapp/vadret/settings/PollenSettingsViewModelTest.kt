package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.successOrNull
import kotlinx.collections.immutable.persistentListOf
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
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.settings.model.PollenAllergenRow
import se.vadretapp.vadret.settings.model.PollenGroup
import se.vadretapp.vadret.settings.model.PollenGroupRows
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PollenSettingsViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(context = testDispatcher)
    private val pollenPreferenceRepository = FakePollenPreferenceRepository()
    private val pollenRepository = FakePollenRepository()
    private val observePollenInSeasonTask = FakeObservePollenInSeasonTask()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): PollenSettingsViewModel {
        val pollenSettingsViewModel = PollenSettingsViewModel(
            getSettingsTask = DefaultGetSettingsTask(
                themeRepository = FakeThemeRepository(),
                languageRepository = FakeLanguageRepository(),
                forecastPreferenceRepository = FakeForecastPreferenceRepository(),
                pollenPreferenceRepository = pollenPreferenceRepository
            ),
            setPollenEnabledTask = DefaultSetPollenEnabledTask(pollenPreferenceRepository = pollenPreferenceRepository),
            setPollenAllergenTask = DefaultSetPollenAllergenTask(pollenPreferenceRepository = pollenPreferenceRepository),
            getPollenTypesTask = DefaultGetPollenTypesTask(pollenRepository = pollenRepository),
            observePollenInSeasonTask = observePollenInSeasonTask,
            coroutineDispatcher = testDispatcher
        )
        pollenSettingsViewModel.onEvent(event = PollenSettingsEvent.OnStart)
        return pollenSettingsViewModel
    }

    @Test
    fun `the pollen with forecasts are grouped the way people know them`() =
        testScope.runTest {
            val pollenSettingsViewModel: PollenSettingsViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                expected = listOf(
                    PollenGroupRows(
                        pollenGroup = PollenGroup.TREES,
                        pollenAllergenRows = persistentListOf(
                            PollenAllergenRow(pollenId = hazelType.pollenId, name = "Hassel", isAllergen = false),
                            PollenAllergenRow(pollenId = birchType.pollenId, name = "Björk", isAllergen = false)
                        )
                    ),
                    PollenGroupRows(
                        pollenGroup = PollenGroup.GRASSES_AND_HERBS,
                        pollenAllergenRows = persistentListOf(
                            PollenAllergenRow(pollenId = grassType.pollenId, name = "Gräs", isAllergen = false)
                        )
                    )
                ),
                actual = pollenSettingsViewModel.state.value.pollenGroupRows
            )
        }

    @Test
    fun `a marked pollen is stored and shown as marked`() =
        testScope.runTest {
            val pollenSettingsViewModel: PollenSettingsViewModel = viewModel()
            advanceUntilIdle()

            pollenSettingsViewModel.onEvent(
                event = PollenSettingsEvent.OnPollenMarked(
                    pollenId = birchType.pollenId,
                    isMarked = true
                )
            )
            advanceUntilIdle()

            assertEquals(
                expected = listOf(hazelType.pollenId to false, grassType.pollenId to false, birchType.pollenId to true).sortedBy { marked: Pair<String, Boolean> -> marked.first },
                actual = pollenSettingsViewModel.state.value.pollenGroupRows
                    .flatMap { pollenGroupRows: PollenGroupRows -> pollenGroupRows.pollenAllergenRows }
                    .map { pollenAllergenRow: PollenAllergenRow -> pollenAllergenRow.pollenId to pollenAllergenRow.isAllergen }
                    .sortedBy { marked: Pair<String, Boolean> -> marked.first }
            )
        }

    @Test
    fun `a second quick tap on a pollen turns it back`() =
        testScope.runTest {
            val pollenSettingsViewModel: PollenSettingsViewModel = viewModel()
            advanceUntilIdle()

            pollenSettingsViewModel.onEvent(
                event = PollenSettingsEvent.OnPollenMarked(
                    pollenId = birchType.pollenId,
                    isMarked = true
                )
            )
            val isMarkedAtOnce: Boolean = pollenSettingsViewModel.isMarked(pollenId = birchType.pollenId)
            pollenSettingsViewModel.onEvent(
                event = PollenSettingsEvent.OnPollenMarked(
                    pollenId = birchType.pollenId,
                    isMarked = false
                )
            )
            advanceUntilIdle()

            assertEquals(
                expected = Triple(
                    first = true,
                    second = false,
                    third = emptySet<String>()
                ),
                actual = Triple(
                    first = isMarkedAtOnce,
                    second = pollenSettingsViewModel.isMarked(pollenId = birchType.pollenId),
                    third = pollenPreferenceRepository.pollenPreferences.successOrNull?.allergenPollenIds
                )
            )
        }

    @Test
    fun `quick taps on two pollen keep both marked`() =
        testScope.runTest {
            val pollenSettingsViewModel: PollenSettingsViewModel = viewModel()
            advanceUntilIdle()

            listOf(birchType, grassType).forEach { pollenType: PollenType ->
                pollenSettingsViewModel.onEvent(
                    event = PollenSettingsEvent.OnPollenMarked(
                        pollenId = pollenType.pollenId,
                        isMarked = true
                    )
                )
            }
            advanceUntilIdle()

            assertEquals(
                expected = true to true,
                actual = pollenSettingsViewModel.isMarked(pollenId = birchType.pollenId) to pollenSettingsViewModel.isMarked(pollenId = grassType.pollenId)
            )
        }

    @Test
    fun `turning the pollen map off is stored and shown`() =
        testScope.runTest {
            val pollenSettingsViewModel: PollenSettingsViewModel = viewModel()
            advanceUntilIdle()

            pollenSettingsViewModel.onEvent(event = PollenSettingsEvent.OnPollenToggled(isPollenEnabled = false))
            advanceUntilIdle()

            assertEquals(expected = false, actual = pollenSettingsViewModel.state.value.isPollenEnabled)
        }

    @Test
    fun `the end of the season is told only when it is known`() =
        testScope.runTest {
            val offSeason: List<Boolean> = listOf(
                true.asRight(),
                false.asRight(),
                Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            ).map { isInSeason: Either<Failure, Boolean> ->
                observePollenInSeasonTask.isInSeason = isInSeason
                val pollenSettingsViewModel: PollenSettingsViewModel = viewModel()
                advanceUntilIdle()
                pollenSettingsViewModel.state.value.isOffSeason
            }

            assertEquals(expected = listOf(false, true, false), actual = offSeason)
        }

    @Test
    fun `pollen types that cannot be loaded are told apart`() =
        testScope.runTest {
            pollenRepository.pollenTypes = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()

            val pollenSettingsViewModel: PollenSettingsViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                expected = true to emptyList<PollenGroupRows>(),
                actual = pollenSettingsViewModel.state.value.arePollenTypesUnavailable to pollenSettingsViewModel.state.value.pollenGroupRows.toList()
            )
        }

    @Test
    fun `pollen preferences that cannot be read show no switch`() =
        testScope.runTest {
            pollenPreferenceRepository.pollenPreferences = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft()

            val pollenSettingsViewModel: PollenSettingsViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(expected = null, actual = pollenSettingsViewModel.state.value.isPollenEnabled)
        }

    private fun PollenSettingsViewModel.isMarked(pollenId: String): Boolean =
        state.value.pollenGroupRows
            .flatMap { pollenGroupRows: PollenGroupRows -> pollenGroupRows.pollenAllergenRows }
            .first { pollenAllergenRow: PollenAllergenRow -> pollenAllergenRow.pollenId == pollenId }
            .isAllergen

    @Test
    fun `a mark that cannot be stored shows what is stored`() =
        testScope.runTest {
            val pollenSettingsViewModel: PollenSettingsViewModel = viewModel()
            advanceUntilIdle()
            pollenPreferenceRepository.writeFailure = Failure(errorCode = ErrorCode.STORAGE_FAILED)

            pollenSettingsViewModel.onEvent(
                event = PollenSettingsEvent.OnPollenMarked(
                    pollenId = birchType.pollenId,
                    isMarked = true
                )
            )
            advanceUntilIdle()

            assertEquals(
                expected = false to emptySet<String>(),
                actual = pollenSettingsViewModel.isMarked(pollenId = birchType.pollenId) to
                    pollenPreferenceRepository.pollenPreferences.successOrNull?.allergenPollenIds
            )
        }
}