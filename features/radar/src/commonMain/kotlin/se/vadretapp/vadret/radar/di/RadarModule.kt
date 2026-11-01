package se.vadretapp.vadret.radar.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.radar.DefaultGetDeviceLocationTask
import se.vadretapp.vadret.radar.DefaultGetRadarTask
import se.vadretapp.vadret.radar.DefaultGetWindForecastTask
import se.vadretapp.vadret.radar.DefaultRadarImageLoader
import se.vadretapp.vadret.radar.GetDeviceLocationTask
import se.vadretapp.vadret.radar.GetRadarTask
import se.vadretapp.vadret.radar.GetWindForecastTask
import se.vadretapp.vadret.radar.RadarImageLoader
import se.vadretapp.vadret.radar.RadarViewModel
import se.vadretapp.vadret.radar.projection.DefaultRadarReprojection
import se.vadretapp.vadret.radar.projection.RadarReprojection
import se.vadretapp.vadret.radar.wind.DefaultWindFieldRenderer
import se.vadretapp.vadret.radar.wind.WindFieldRenderer

val radarModule: Module = module {
    factoryOf(::DefaultGetDeviceLocationTask) { bind<GetDeviceLocationTask>() }
    factoryOf(::DefaultGetRadarTask) { bind<GetRadarTask>() }
    factoryOf(::DefaultGetWindForecastTask) { bind<GetWindForecastTask>() }
    single<WindFieldRenderer> { DefaultWindFieldRenderer(coroutineDispatcher = get()) }
    single<RadarReprojection> { DefaultRadarReprojection() }
    single<RadarImageLoader> {
        DefaultRadarImageLoader(
            platformContext = get(),
            radarReprojection = get(),
            httpClient = inject()
        )
    }
    factoryOf(::RadarViewModel)
}