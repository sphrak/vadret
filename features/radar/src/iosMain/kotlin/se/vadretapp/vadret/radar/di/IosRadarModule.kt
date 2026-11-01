package se.vadretapp.vadret.radar.di

import coil3.PlatformContext
import org.koin.core.module.Module
import org.koin.dsl.module
import se.vadretapp.vadret.radar.precipitation.MotionPrecipitationRenderer
import se.vadretapp.vadret.radar.precipitation.PrecipitationRenderer

val iosRadarModule: Module = module {
    single<PlatformContext> { PlatformContext.INSTANCE }
    single<PrecipitationRenderer> { MotionPrecipitationRenderer(coroutineDispatcher = get()) }
}