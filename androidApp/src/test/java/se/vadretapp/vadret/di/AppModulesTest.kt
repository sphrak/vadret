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
import se.vadretapp.vadret.forecastwarning.WarningDetailViewModel
import se.vadretapp.vadret.forecastwarning.WarningStoryViewModel
import se.vadretapp.vadret.forecastwarning.WarningViewModel
import se.vadretapp.vadret.radar.RadarViewModel
import se.vadretapp.vadret.settings.AboutViewModel
import se.vadretapp.vadret.settings.DeveloperViewModel
import se.vadretapp.vadret.settings.LabsViewModel
import se.vadretapp.vadret.settings.LicensesViewModel
import se.vadretapp.vadret.settings.PlacesViewModel
import se.vadretapp.vadret.settings.PollenSettingsViewModel
import se.vadretapp.vadret.settings.SettingsViewModel
import se.vadretapp.vadret.weatherforecast.WeatherForecastViewModel
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class AppModulesTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val cacheDirectory: File = Files.createTempDirectory("app-modules-test").toFile()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        cacheDirectory.deleteRecursively()
    }

    private fun koin(): Koin {
        val context: Context = mockk(relaxed = true)
        every { context.applicationContext } returns context
        every { context.cacheDir } returns cacheDirectory
        every { context.filesDir } returns cacheDirectory
        every { context.getDatabasePath(any()) } answers { File(cacheDirectory, firstArg<String>()) }
        every { context.getSystemService(LocationManager::class.java) } returns mockk()
        return koinApplication {
            modules(
                modules = appModules(context = context) + module {
                    factory<CoroutineDispatcher> { testDispatcher }
                }
            )
        }.koin
    }

    @Test
    fun `every view model can be built from the app modules`() {
        val koin: Koin = koin()

        assertNotNull(actual = koin.get<RadarViewModel>())
        assertNotNull(actual = koin.get<SettingsViewModel>())
        assertNotNull(actual = koin.get<AboutViewModel>())
        assertNotNull(actual = koin.get<DeveloperViewModel>())
        assertNotNull(actual = koin.get<LabsViewModel>())
        assertNotNull(actual = koin.get<LicensesViewModel>())
        assertNotNull(actual = koin.get<PlacesViewModel>())
        assertNotNull(actual = koin.get<PollenSettingsViewModel>())
        assertNotNull(actual = koin.get<WarningViewModel>())
        assertNotNull(actual = koin.get<WarningDetailViewModel>())
        assertNotNull(actual = koin.get<WarningStoryViewModel>())
        assertNotNull(actual = koin.get<WeatherForecastViewModel>())
    }
}