package se.vadretapp.vadret.di

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
import se.vadretapp.vadret.lifecycle.AppStartHook
import se.vadretapp.vadret.main.MainViewModel
import se.vadretapp.vadret.model.AppInfo
import se.vadretapp.vadret.pushalert.di.pushAlertModule
import se.vadretapp.vadret.pushalert.usecase.OpenPushAlertTask
import se.vadretapp.vadret.pushalert.usecase.SyncPushAlertsTask
import se.vadretapp.vadret.radar.RadarViewModel
import se.vadretapp.vadret.radar.di.iosRadarModule
import se.vadretapp.vadret.radar.di.radarModule
import se.vadretapp.vadret.settings.SettingsViewModel
import se.vadretapp.vadret.ui.SettingsSection
import se.vadretapp.vadret.weatherforecast.WeatherForecastViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class IosModulesTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val appInfo = AppInfo(
        applicationId = "se.vadretapp.vadret",
        versionName = "3.1.0-test",
        isDebug = true,
        isDeveloperBuild = true
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun koin(): Koin =
        koinApplication {
            modules(
                modules = sharedModules + listOf(
                    iosPlatformModule(appInfo = appInfo),
                    radarModule,
                    iosRadarModule,
                    pushAlertModule,
                    iosPushAlertModule
                ) + module {
                    factory<CoroutineDispatcher> { testDispatcher }
                }
            )
        }.koin

    @Test
    fun `every view model can be built from the ios modules`() {
        val koin: Koin = koin()

        assertNotNull(actual = koin.get<MainViewModel>())
        assertNotNull(actual = koin.get<RadarViewModel>())
        assertNotNull(actual = koin.get<SettingsViewModel>())
        assertNotNull(actual = koin.get<WarningViewModel>())
        assertNotNull(actual = koin.get<WarningDetailViewModel>())
        assertNotNull(actual = koin.get<WarningStoryViewModel>())
        assertNotNull(actual = koin.get<WeatherForecastViewModel>())
    }

    @Test
    fun `push alerts can be built from the ios modules`() {
        val koin: Koin = koin()

        assertNotNull(actual = koin.get<SyncPushAlertsTask>())
        assertNotNull(actual = koin.get<OpenPushAlertTask>())
        assertEquals(
            expected = 1 to 1,
            actual = koin.getAll<SettingsSection>().size to koin.getAll<AppStartHook>().size
        )
    }
}