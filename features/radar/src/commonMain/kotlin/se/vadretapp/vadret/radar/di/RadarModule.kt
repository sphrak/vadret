package se.vadretapp.vadret.radar.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.model.AppInfo
import se.vadretapp.vadret.radar.DefaultDownloadRadarDayTask
import se.vadretapp.vadret.radar.DefaultGetDeviceLocationTask
import se.vadretapp.vadret.radar.DefaultGetPollenMapTask
import se.vadretapp.vadret.radar.DefaultGetRadarTask
import se.vadretapp.vadret.radar.DefaultGetWindForecastTask
import se.vadretapp.vadret.radar.DefaultObservePollenInSeasonTask
import se.vadretapp.vadret.radar.DefaultObservePollenPreferencesTask
import se.vadretapp.vadret.radar.DefaultRadarImageLoader
import se.vadretapp.vadret.radar.DefaultSetPollenHibernationNoticeShownTask
import se.vadretapp.vadret.radar.DownloadRadarDayTask
import se.vadretapp.vadret.radar.GetDeviceLocationTask
import se.vadretapp.vadret.radar.GetPollenMapTask
import se.vadretapp.vadret.radar.GetRadarTask
import se.vadretapp.vadret.radar.GetWindForecastTask
import se.vadretapp.vadret.radar.ObservePollenInSeasonTask
import se.vadretapp.vadret.radar.ObservePollenPreferencesTask
import se.vadretapp.vadret.radar.RadarImageLoader
import se.vadretapp.vadret.radar.RadarViewModel
import se.vadretapp.vadret.radar.SetPollenHibernationNoticeShownTask
import se.vadretapp.vadret.radar.pollen.DefaultPollenCloudRenderer
import se.vadretapp.vadret.radar.pollen.PollenCloudRenderer
import se.vadretapp.vadret.radar.projection.DefaultRadarReprojection
import se.vadretapp.vadret.radar.projection.RadarReprojection
import se.vadretapp.vadret.radar.simulation.EndOfSeasonSimulatingObservePollenInSeasonTask
import se.vadretapp.vadret.radar.wind.DefaultWindFieldRenderer
import se.vadretapp.vadret.radar.wind.WindFieldRenderer

val radarModule: Module = module {
    factoryOf(::DefaultGetDeviceLocationTask) { bind<GetDeviceLocationTask>() }
    factoryOf(::DefaultGetPollenMapTask) { bind<GetPollenMapTask>() }
    factoryOf(::DefaultObservePollenPreferencesTask) { bind<ObservePollenPreferencesTask>() }
    factory<ObservePollenInSeasonTask> {
        val observePollenInSeasonTask: ObservePollenInSeasonTask = DefaultObservePollenInSeasonTask(pollenRepository = get())
        if (get<AppInfo>().isDeveloperBuild) {
            EndOfSeasonSimulatingObservePollenInSeasonTask(
                observePollenInSeasonTask = observePollenInSeasonTask,
                pollenPreferenceRepository = get()
            )
        } else {
            observePollenInSeasonTask
        }
    }
    factoryOf(::DefaultSetPollenHibernationNoticeShownTask) { bind<SetPollenHibernationNoticeShownTask>() }
    factoryOf(::DefaultGetRadarTask) { bind<GetRadarTask>() }
    factoryOf(::DefaultDownloadRadarDayTask) { bind<DownloadRadarDayTask>() }
    factoryOf(::DefaultGetWindForecastTask) { bind<GetWindForecastTask>() }
    single<WindFieldRenderer> { DefaultWindFieldRenderer(coroutineDispatcher = get()) }
    single<PollenCloudRenderer> { DefaultPollenCloudRenderer(coroutineDispatcher = get()) }
    single<RadarReprojection> { DefaultRadarReprojection() }
    single<RadarImageLoader> {
        DefaultRadarImageLoader(
            platformContext = get(),
            radarRepository = get(),
            radarReprojection = get()
        )
    }
    factoryOf(::RadarViewModel)
}