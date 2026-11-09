package se.vadretapp.vadret.setting

import androidx.datastore.core.IOException
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.local.DefaultSettingLocalDataSource
import se.vadretapp.vadret.testing.testPreferencesDataStore
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSettingRepositoryNativeNavigationTest {

    private fun TestScope.settingRepository(): SettingRepository =
        DefaultSettingRepository(
            settingLocalDataSource = DefaultSettingLocalDataSource(dataStore = testPreferencesDataStore(scope = backgroundScope)),
            systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()
        )

    @Test
    fun `nothing stored keeps the app in its own navigation`() =
        runTest {
            val settingRepository: SettingRepository = settingRepository()

            assertEquals(
                expected = false.asRight() to false.asRight(),
                actual = settingRepository.isNativeNavigationEnabled() to settingRepository.observeNativeNavigationEnabled().first()
            )
        }

    @Test
    fun `native navigation reads back as it was chosen`() =
        runTest {
            val settingRepository: SettingRepository = settingRepository()

            val readBack: List<Pair<Either<Failure, Boolean>, Either<Failure, Boolean>>> =
                listOf(true, false).map { isEnabled: Boolean ->
                    settingRepository.setNativeNavigationEnabled(isEnabled = isEnabled)
                    settingRepository.isNativeNavigationEnabled() to settingRepository.observeNativeNavigationEnabled().first()
                }

            assertEquals(
                expected = listOf(true.asRight() to true.asRight(), false.asRight() to false.asRight()),
                actual = readBack
            )
        }

    @Test
    fun `storage that cannot be read or written is a storage failure`() =
        runTest {
            val settingRepository = DefaultSettingRepository(
                settingLocalDataSource = FailingSettingLocalDataSource(exception = IOException("disk full", null)),
                systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()
            )

            val results: List<Either<Failure, Any>> = listOf(
                settingRepository.isNativeNavigationEnabled(),
                settingRepository.observeNativeNavigationEnabled().first(),
                settingRepository.setNativeNavigationEnabled(isEnabled = true)
            )

            assertEquals(
                expected = List(size = results.size) { Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft() },
                actual = results
            )
        }
}