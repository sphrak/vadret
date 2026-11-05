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
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.di.model.AppInfo
import se.vadretapp.vadret.settings.model.AboutRow
import se.vadretapp.vadret.settings.model.AboutRowAction
import se.vadretapp.vadret.settings.model.RowSummary
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.developer_already_unlocked
import se.vadretapp.vadret.ui.resources.developer_unlocked
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val VERSION_TAPS_TO_UNLOCK = 7

@OptIn(ExperimentalCoroutinesApi::class)
class AboutViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(context = testDispatcher)
    private val changelogRepository = FakeChangelogRepository()
    private val developerModeRepository = FakeDeveloperModeRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(appInfo: AppInfo = releaseAppInfo): AboutViewModel {
        val aboutViewModel = AboutViewModel(
            getChangelogTask = DefaultGetChangelogTask(changelogRepository = changelogRepository),
            observeDeveloperModeTask = DefaultObserveDeveloperModeTask(
                developerModeRepository = developerModeRepository,
                appInfo = appInfo
            ),
            unlockDeveloperModeTask = DefaultUnlockDeveloperModeTask(developerModeRepository = developerModeRepository),
            appInfo = appInfo,
            coroutineDispatcher = testDispatcher
        )
        aboutViewModel.onEvent(event = AboutEvent.OnStart)
        return aboutViewModel
    }

    private fun AboutViewModel.tapVersion(times: Int) {
        repeat(times = times) { onEvent(event = AboutEvent.OnVersionClicked) }
    }

    @Test
    fun `the version counts taps and the licenses are a row of their own`() =
        testScope.runTest {
            val aboutViewModel: AboutViewModel = viewModel()

            val versionRow: AboutRow = aboutViewModel.state.value.aboutRows.first()

            assertEquals(
                expected = RowSummary.Plain(text = "3.1.0-test") to AboutRowAction.CountVersionTap,
                actual = versionRow.summary to versionRow.action
            )
            assertEquals(expected = AboutRowAction.ShowLicenses, actual = aboutViewModel.state.value.aboutRows.last().action)
        }

    @Test
    fun `six taps on the version leave developer mode locked`() =
        testScope.runTest {
            val aboutViewModel: AboutViewModel = viewModel()
            advanceUntilIdle()

            aboutViewModel.tapVersion(times = VERSION_TAPS_TO_UNLOCK - 1)
            advanceUntilIdle()

            assertEquals(
                expected = 0 to null,
                actual = developerModeRepository.unlocks to aboutViewModel.state.value.toastResource
            )
        }

    @Test
    fun `the seventh tap on the version unlocks developer mode and says so once`() =
        testScope.runTest {
            val aboutViewModel: AboutViewModel = viewModel()
            advanceUntilIdle()

            aboutViewModel.tapVersion(times = VERSION_TAPS_TO_UNLOCK)
            advanceUntilIdle()
            val toastResource: StringResource? = aboutViewModel.state.value.toastResource
            aboutViewModel.onEvent(event = AboutEvent.OnToastShown)

            assertEquals(
                expected = Triple(
                    first = 1,
                    second = true,
                    third = Res.string.developer_unlocked
                ),
                actual = Triple(
                    first = developerModeRepository.unlocks,
                    second = developerModeRepository.isUnlocked.value,
                    third = toastResource
                )
            )
            assertEquals(expected = null, actual = aboutViewModel.state.value.toastResource)
        }

    @Test
    fun `taps before the stored choice is read are not counted`() =
        testScope.runTest {
            developerModeRepository.isUnlocked.value = true
            val aboutViewModel: AboutViewModel = viewModel()

            aboutViewModel.tapVersion(times = VERSION_TAPS_TO_UNLOCK)
            advanceUntilIdle()

            assertEquals(
                expected = 0 to null,
                actual = developerModeRepository.unlocks to aboutViewModel.state.value.toastResource
            )
        }

    @Test
    fun `a tap once in developer mode says it is already unlocked`() =
        testScope.runTest {
            val aboutViewModel: AboutViewModel = viewModel(appInfo = developerAppInfo)
            advanceUntilIdle()

            aboutViewModel.tapVersion(times = 1)
            advanceUntilIdle()

            assertEquals(
                expected = 0 to Res.string.developer_already_unlocked,
                actual = developerModeRepository.unlocks to aboutViewModel.state.value.toastResource
            )
        }

    @Test
    fun `an unlock that fails can be tapped again`() =
        testScope.runTest {
            developerModeRepository.unlockResult = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft()
            val aboutViewModel: AboutViewModel = viewModel()
            advanceUntilIdle()

            aboutViewModel.tapVersion(times = VERSION_TAPS_TO_UNLOCK)
            advanceUntilIdle()
            aboutViewModel.tapVersion(times = VERSION_TAPS_TO_UNLOCK)
            advanceUntilIdle()

            assertEquals(
                expected = 2 to null,
                actual = developerModeRepository.unlocks to aboutViewModel.state.value.toastResource
            )
        }

    @Test
    fun `the changelog is shown until it is dismissed`() =
        testScope.runTest {
            val aboutViewModel: AboutViewModel = viewModel()
            advanceUntilIdle()

            aboutViewModel.onEvent(event = AboutEvent.OnChangelogClicked)
            val isShown: Boolean = aboutViewModel.state.value.isChangelogVisible
            aboutViewModel.onEvent(event = AboutEvent.OnChangelogDismissed)

            assertEquals(
                expected = true to false,
                actual = isShown to aboutViewModel.state.value.isChangelogVisible
            )
        }

    @Test
    fun `a changelog that cannot be read is never shown`() =
        testScope.runTest {
            changelogRepository.changelog = Failure(errorCode = ErrorCode.FILE_READ_FAILED).asLeft()
            val aboutViewModel: AboutViewModel = viewModel()
            advanceUntilIdle()

            aboutViewModel.onEvent(event = AboutEvent.OnChangelogClicked)

            assertEquals(expected = false, actual = aboutViewModel.state.value.isChangelogVisible)
        }
}