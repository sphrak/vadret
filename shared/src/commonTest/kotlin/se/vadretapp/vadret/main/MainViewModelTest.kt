package se.vadretapp.vadret.main

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.withRecordedLogs
import se.vadretapp.vadret.warning.usecase.GetReferencedWarningIdentifiersTask
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FakeObserveThemeTask : ObserveThemeTask {

    val themes = MutableSharedFlow<Either<Failure, Theme>>()

    override fun invoke(): Flow<Either<Failure, Theme>> = themes
}

private class FakeObserveLanguageTask : ObserveLanguageTask {

    val languages = MutableSharedFlow<Either<Failure, AppLanguage>>()

    override fun invoke(): Flow<Either<Failure, AppLanguage>> = languages
}

private class FakeObserveHourFormatTask : ObserveHourFormatTask {

    val hourFormats = MutableSharedFlow<Either<Failure, HourFormat>>()

    override fun invoke(): Flow<Either<Failure, HourFormat>> = hourFormats
}

private class FakeGetReferencedWarningIdentifiersTask : GetReferencedWarningIdentifiersTask {

    var result: Either<Failure, List<String>> = listOf("smhi-1-12").asRight()

    override suspend fun invoke(warningReference: WarningReference): Either<Failure, List<String>> = result
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val observeThemeTask = FakeObserveThemeTask()
    private val observeLanguageTask = FakeObserveLanguageTask()
    private val observeHourFormatTask = FakeObserveHourFormatTask()
    private val appStartHook = CountingAppStartHook()
    private val openedWarningReferences = OpenedWarningReferences()
    private val getReferencedWarningIdentifiersTask = FakeGetReferencedWarningIdentifiersTask()
    private val mainViewModel = MainViewModel(
        observeThemeTask = observeThemeTask,
        observeLanguageTask = observeLanguageTask,
        observeHourFormatTask = observeHourFormatTask,
        appStartHooks = listOf(appStartHook),
        openedWarningReferences = openedWarningReferences,
        getReferencedWarningIdentifiersTask = getReferencedWarningIdentifiersTask,
        coroutineDispatcher = testDispatcher
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `every start hook runs once however often the app starts`() =
        testScope.runTest {
            mainViewModel.onEvent(event = MainEvent.OnStart)
            mainViewModel.onEvent(event = MainEvent.OnStart)
            advanceUntilIdle()

            assertEquals(
                expected = 1,
                actual = appStartHook.starts.value
            )
        }

    @Test
    fun `the theme is null until started and then follows every change`() =
        testScope.runTest {
            val seen = mutableListOf<Theme?>()
            val job = launch { mainViewModel.theme.collect { theme: Theme? -> seen.add(theme) } }
            advanceUntilIdle()

            mainViewModel.onEvent(event = MainEvent.OnStart)
            advanceUntilIdle()
            observeThemeTask.themes.emit(value = Theme.DARK.asRight())
            advanceUntilIdle()
            observeThemeTask.themes.emit(value = Failure(errorCode = ErrorCode.UNKNOWN).asLeft())
            advanceUntilIdle()
            job.cancel()

            assertEquals(
                expected = listOf(null, Theme.DARK, Theme.SYSTEM),
                actual = seen
            )
        }

    @Test
    fun `the language is null until started and then follows every change`() =
        testScope.runTest {
            val seen = mutableListOf<AppLanguage?>()
            val job = launch { mainViewModel.language.collect { language: AppLanguage? -> seen.add(language) } }
            advanceUntilIdle()

            mainViewModel.onEvent(event = MainEvent.OnStart)
            advanceUntilIdle()
            observeLanguageTask.languages.emit(value = AppLanguage.SWEDISH.asRight())
            advanceUntilIdle()
            observeLanguageTask.languages.emit(value = Failure(errorCode = ErrorCode.UNKNOWN).asLeft())
            advanceUntilIdle()
            job.cancel()

            assertEquals(
                expected = listOf(null, AppLanguage.SWEDISH, AppLanguage.SYSTEM),
                actual = seen
            )
        }

    @Test
    fun `the clock is null until started and then follows every change, falling back to 24 hours`() =
        testScope.runTest {
            val seen = mutableListOf<HourFormat?>()
            val job = launch { mainViewModel.hourFormat.collect { hourFormat: HourFormat? -> seen.add(hourFormat) } }
            advanceUntilIdle()

            mainViewModel.onEvent(event = MainEvent.OnStart)
            advanceUntilIdle()
            observeHourFormatTask.hourFormats.emit(value = HourFormat.TWELVE_HOUR.asRight())
            advanceUntilIdle()
            observeHourFormatTask.hourFormats.emit(value = Failure(errorCode = ErrorCode.UNKNOWN).asLeft())
            advanceUntilIdle()
            job.cancel()

            assertEquals(
                expected = listOf(null, HourFormat.TWELVE_HOUR, HourFormat.TWENTY_FOUR_HOUR),
                actual = seen
            )
        }

    @Test
    fun `nothing is observed before the start`() =
        testScope.runTest {
            advanceUntilIdle()

            assertEquals(
                expected = listOf(0, 0),
                actual = listOf(
                    observeThemeTask.themes.subscriptionCount.value,
                    observeLanguageTask.languages.subscriptionCount.value
                )
            )
            assertNull(actual = mainViewModel.theme.value)
            assertNull(actual = mainViewModel.language.value)
        }

    @Test
    fun `the theme and language are observed once however often it is started`() =
        testScope.runTest {
            mainViewModel.onEvent(event = MainEvent.OnStart)
            mainViewModel.onEvent(event = MainEvent.OnStart)
            advanceUntilIdle()

            assertEquals(
                expected = listOf(1, 1),
                actual = listOf(
                    observeThemeTask.themes.subscriptionCount.value,
                    observeLanguageTask.languages.subscriptionCount.value
                )
            )
        }

    @Test
    fun `a notification opened before the start opens the story it refers to once started`() =
        testScope.runTest {
            val opened = mutableListOf<List<String>>()
            val job = launch { mainViewModel.openedWarningIdentifiers.collect { warningIdentifiers: List<String> -> opened.add(warningIdentifiers) } }
            openedWarningReferences.open(warningReference = WarningReference.SmhiWarningArea(warningAreaId = 12))
            advanceUntilIdle()
            val beforeStart: List<List<String>> = opened.toList()

            mainViewModel.onEvent(event = MainEvent.OnStart)
            advanceUntilIdle()
            job.cancel()

            assertEquals(
                expected = emptyList<List<String>>() to listOf(listOf("smhi-1-12")),
                actual = beforeStart to opened
            )
        }

    @Test
    fun `a notification about a warning that is gone or cannot be found opens only the app`() =
        testScope.runTest {
            val opened = mutableListOf<List<String>>()
            val job = launch { mainViewModel.openedWarningIdentifiers.collect { warningIdentifiers: List<String> -> opened.add(warningIdentifiers) } }
            mainViewModel.onEvent(event = MainEvent.OnStart)
            advanceUntilIdle()

            withRecordedLogs { recordingLogSink ->
                getReferencedWarningIdentifiersTask.result = emptyList<String>().asRight()
                openedWarningReferences.open(warningReference = WarningReference.SmhiWarningArea(warningAreaId = 12))
                advanceUntilIdle()
                getReferencedWarningIdentifiersTask.result = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
                openedWarningReferences.open(warningReference = WarningReference.SmhiWarningArea(warningAreaId = 13))
                advanceUntilIdle()
                job.cancel()

                assertEquals(
                    expected = emptyList<List<String>>() to listOf(
                        "An opened notification refers to a warning that is gone, so only the app opens",
                        "The story of an opened notification cannot be found: NETWORK_UNAVAILABLE"
                    ),
                    actual = opened to recordingLogSink.lines.map { line: LogLine -> line.message }
                )
            }
        }
}