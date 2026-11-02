package se.vadretapp.vadret.di

import org.koin.core.module.Module
import se.vadretapp.vadret.followedplace.di.followedPlaceTaskModule
import se.vadretapp.vadret.forecastwarning.di.warningModule
import se.vadretapp.vadret.settings.di.settingsModule
import se.vadretapp.vadret.warning.di.warningTaskModule
import se.vadretapp.vadret.weatherforecast.di.weatherForecastModule

val sharedModules: List<Module> = listOf(
    coreModule,
    followedPlaceTaskModule,
    changelogModule,
    networkModule,
    repositoryModule,
    mainModule,
    settingsModule,
    warningModule,
    warningTaskModule,
    weatherForecastModule
)