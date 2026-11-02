package se.vadretapp.vadret.weatherforecast.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.weatherforecast.AddFavoriteTask
import se.vadretapp.vadret.weatherforecast.DefaultAddFavoriteTask
import se.vadretapp.vadret.weatherforecast.DefaultGetLocationModeTask
import se.vadretapp.vadret.weatherforecast.DefaultGetPlaceForecastTask
import se.vadretapp.vadret.weatherforecast.DefaultGetWeatherForecastTask
import se.vadretapp.vadret.weatherforecast.DefaultObserveForecastPreferencesTask
import se.vadretapp.vadret.weatherforecast.DefaultObserveLocationChoicesTask
import se.vadretapp.vadret.weatherforecast.DefaultRemoveFavoriteTask
import se.vadretapp.vadret.weatherforecast.DefaultSearchPlacesTask
import se.vadretapp.vadret.weatherforecast.DefaultSelectFavoriteTask
import se.vadretapp.vadret.weatherforecast.DefaultSelectPlaceTask
import se.vadretapp.vadret.weatherforecast.DefaultSetLocationModeTask
import se.vadretapp.vadret.weatherforecast.GetLocationModeTask
import se.vadretapp.vadret.weatherforecast.GetPlaceForecastTask
import se.vadretapp.vadret.weatherforecast.GetWeatherForecastTask
import se.vadretapp.vadret.weatherforecast.ObserveForecastPreferencesTask
import se.vadretapp.vadret.weatherforecast.ObserveLocationChoicesTask
import se.vadretapp.vadret.weatherforecast.PlaceIslandViewModel
import se.vadretapp.vadret.weatherforecast.RemoveFavoriteTask
import se.vadretapp.vadret.weatherforecast.SearchPlacesTask
import se.vadretapp.vadret.weatherforecast.SelectFavoriteTask
import se.vadretapp.vadret.weatherforecast.SelectPlaceTask
import se.vadretapp.vadret.weatherforecast.SetLocationModeTask
import se.vadretapp.vadret.weatherforecast.WeatherForecastViewModel

val weatherForecastModule: Module = module {
    factoryOf(::DefaultAddFavoriteTask) { bind<AddFavoriteTask>() }
    factoryOf(::DefaultGetLocationModeTask) { bind<GetLocationModeTask>() }
    factoryOf(::DefaultGetPlaceForecastTask) { bind<GetPlaceForecastTask>() }
    factoryOf(::DefaultGetWeatherForecastTask) { bind<GetWeatherForecastTask>() }
    factoryOf(::DefaultObserveForecastPreferencesTask) { bind<ObserveForecastPreferencesTask>() }
    factoryOf(::DefaultObserveLocationChoicesTask) { bind<ObserveLocationChoicesTask>() }
    factoryOf(::DefaultRemoveFavoriteTask) { bind<RemoveFavoriteTask>() }
    factoryOf(::DefaultSearchPlacesTask) { bind<SearchPlacesTask>() }
    factoryOf(::DefaultSelectFavoriteTask) { bind<SelectFavoriteTask>() }
    factoryOf(::DefaultSelectPlaceTask) { bind<SelectPlaceTask>() }
    factoryOf(::DefaultSetLocationModeTask) { bind<SetLocationModeTask>() }
    factoryOf(::PlaceIslandViewModel)
    factoryOf(::WeatherForecastViewModel)
}