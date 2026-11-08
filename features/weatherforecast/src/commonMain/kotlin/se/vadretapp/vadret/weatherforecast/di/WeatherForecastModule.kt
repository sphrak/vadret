package se.vadretapp.vadret.weatherforecast.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.weatherforecast.PlaceIslandViewModel
import se.vadretapp.vadret.weatherforecast.PlaceSearchViewModel
import se.vadretapp.vadret.weatherforecast.WeatherForecastViewModel
import se.vadretapp.vadret.weatherforecast.usecase.DefaultGetLocationModeTask
import se.vadretapp.vadret.weatherforecast.usecase.DefaultObserveForecastPreferencesTask
import se.vadretapp.vadret.weatherforecast.usecase.DefaultObservePlaceForecastTask
import se.vadretapp.vadret.weatherforecast.usecase.DefaultObserveWeatherForecastTask
import se.vadretapp.vadret.weatherforecast.usecase.GetLocationModeTask
import se.vadretapp.vadret.weatherforecast.usecase.ObserveForecastPreferencesTask
import se.vadretapp.vadret.weatherforecast.usecase.ObservePlaceForecastTask
import se.vadretapp.vadret.weatherforecast.usecase.ObserveWeatherForecastTask

val weatherForecastModule: Module = module {
    factoryOf(::DefaultGetLocationModeTask) { bind<GetLocationModeTask>() }
    factoryOf(::DefaultObservePlaceForecastTask) { bind<ObservePlaceForecastTask>() }
    factoryOf(::DefaultObserveWeatherForecastTask) { bind<ObserveWeatherForecastTask>() }
    factoryOf(::DefaultObserveForecastPreferencesTask) { bind<ObserveForecastPreferencesTask>() }
    factoryOf(::PlaceIslandViewModel)
    factoryOf(::PlaceSearchViewModel)
    factoryOf(::WeatherForecastViewModel)
}