package se.vadretapp.vadret.weatherforecast.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.weatherforecast.DefaultGetLocationModeTask
import se.vadretapp.vadret.weatherforecast.DefaultObserveForecastPreferencesTask
import se.vadretapp.vadret.weatherforecast.DefaultObservePlaceForecastTask
import se.vadretapp.vadret.weatherforecast.DefaultObserveWeatherForecastTask
import se.vadretapp.vadret.weatherforecast.GetLocationModeTask
import se.vadretapp.vadret.weatherforecast.ObserveForecastPreferencesTask
import se.vadretapp.vadret.weatherforecast.ObservePlaceForecastTask
import se.vadretapp.vadret.weatherforecast.ObserveWeatherForecastTask
import se.vadretapp.vadret.weatherforecast.PlaceIslandViewModel
import se.vadretapp.vadret.weatherforecast.WeatherForecastViewModel

val weatherForecastModule: Module = module {
    factoryOf(::DefaultGetLocationModeTask) { bind<GetLocationModeTask>() }
    factoryOf(::DefaultObservePlaceForecastTask) { bind<ObservePlaceForecastTask>() }
    factoryOf(::DefaultObserveWeatherForecastTask) { bind<ObserveWeatherForecastTask>() }
    factoryOf(::DefaultObserveForecastPreferencesTask) { bind<ObserveForecastPreferencesTask>() }
    factoryOf(::PlaceIslandViewModel)
    factoryOf(::WeatherForecastViewModel)
}