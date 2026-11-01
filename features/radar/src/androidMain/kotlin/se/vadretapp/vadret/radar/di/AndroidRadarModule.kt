package se.vadretapp.vadret.radar.di

import android.os.Build
import org.koin.core.module.Module
import org.koin.dsl.module
import se.vadretapp.vadret.radar.precipitation.ColoredPrecipitationRenderer
import se.vadretapp.vadret.radar.precipitation.MotionPrecipitationRenderer
import se.vadretapp.vadret.radar.precipitation.PrecipitationRenderer

val androidRadarModule: Module = module {
    single<PrecipitationRenderer> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MotionPrecipitationRenderer(coroutineDispatcher = get())
        } else {
            ColoredPrecipitationRenderer(coroutineDispatcher = get())
        }
    }
}