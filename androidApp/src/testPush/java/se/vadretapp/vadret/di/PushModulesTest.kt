package se.vadretapp.vadret.di

import android.content.Context
import android.location.LocationManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import se.vadretapp.vadret.lifecycle.AppStartHook
import se.vadretapp.vadret.pushalert.usecase.OpenPushAlertTask
import se.vadretapp.vadret.pushalert.usecase.SyncPushAlertsTask
import se.vadretapp.vadret.ui.SettingsSection
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class PushModulesTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val directory: File = Files.createTempDirectory("push-modules-test").toFile()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        directory.deleteRecursively()
    }

    @Test
    fun `push alerts can be built from the push modules`() {
        val context: Context = mockk(relaxed = true)
        every { context.applicationContext } returns context
        every { context.cacheDir } returns directory
        every { context.filesDir } returns directory
        every { context.noBackupFilesDir } returns directory
        every { context.getDatabasePath(any()) } answers { File(directory, firstArg<String>()) }
        every { context.getSystemService(LocationManager::class.java) } returns mockk()
        val koin: Koin = koinApplication {
            modules(
                modules = appModules(context = context) + module {
                    factory<CoroutineDispatcher> { testDispatcher }
                }
            )
        }.koin

        assertNotNull(actual = koin.get<SyncPushAlertsTask>())
        assertNotNull(actual = koin.get<OpenPushAlertTask>())
        assertEquals(
            expected = 1 to 1,
            actual = koin.getAll<SettingsSection>().size to koin.getAll<AppStartHook>().size
        )
    }
}