package se.vadretapp.vadret.radar.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.radar.DefaultRadarImageLoader
import se.vadretapp.vadret.radar.RadarImageLoader
import se.vadretapp.vadret.radar.RadarViewModel
import se.vadretapp.vadret.radar.pollen.DefaultPollenCloudRenderer
import se.vadretapp.vadret.radar.pollen.PollenCloudRenderer
import se.vadretapp.vadret.radar.projection.DefaultRadarReprojection
import se.vadretapp.vadret.radar.projection.RadarReprojection
import se.vadretapp.vadret.radar.usecase.DefaultDownloadRadarDayTask
import se.vadretapp.vadret.radar.usecase.DefaultGetDeviceLocationTask
import se.vadretapp.vadret.radar.usecase.DefaultGetPollenMapTask
import se.vadretapp.vadret.radar.usecase.DefaultObservePollenPreferencesTask
import se.vadretapp.vadret.radar.usecase.DefaultObserveRadarTask
import se.vadretapp.vadret.radar.usecase.DefaultObserveWindForecastTask
import se.vadretapp.vadret.radar.usecase.DefaultSetPollenHibernationNoticeShownTask
import se.vadretapp.vadret.radar.usecase.DownloadRadarDayTask
import se.vadretapp.vadret.radar.usecase.GetDeviceLocationTask
import se.vadretapp.vadret.radar.usecase.GetPollenMapTask
import se.vadretapp.vadret.radar.usecase.ObservePollenPreferencesTask
import se.vadretapp.vadret.radar.usecase.ObserveRadarTask
import se.vadretapp.vadret.radar.usecase.ObserveWindForecastTask
import se.vadretapp.vadret.radar.usecase.SetPollenHibernationNoticeShownTask
import se.vadretapp.vadret.radar.wind.DefaultWindFieldRenderer
import se.vadretapp.vadret.radar.wind.WindFieldRenderer

val radarModule: Module = module {
    factoryOf(::DefaultGetDeviceLocationTask) { bind<GetDeviceLocationTask>() }
    factoryOf(::DefaultGetPollenMapTask) { bind<GetPollenMapTask>() }
    factoryOf(::DefaultObservePollenPreferencesTask) { bind<ObservePollenPreferencesTask>() }
    factoryOf(::DefaultSetPollenHibernationNoticeShownTask) { bind<SetPollenHibernationNoticeShownTask>() }
    factoryOf(::DefaultObserveRadarTask) { bind<ObserveRadarTask>() }
    factoryOf(::DefaultDownloadRadarDayTask) { bind<DownloadRadarDayTask>() }
    factoryOf(::DefaultObserveWindForecastTask) { bind<ObserveWindForecastTask>() }
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