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
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.theme.model.Theme
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

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val observeThemeTask = FakeObserveThemeTask()
    private val observeLanguageTask = FakeObserveLanguageTask()
    private val appStartHook = CountingAppStartHook()
    private val mainViewModel = MainViewModel(
        observeThemeTask = observeThemeTask,
        observeLanguageTask = observeLanguageTask,
        appStartHooks = listOf(appStartHook),
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
}