package fi.kroon.vadret.di

import fi.kroon.vadret.core.resources.Res
import fi.kroon.vadret.forecastpreference.ForecastPreferenceRepository
import fi.kroon.vadret.language.LanguageRepository
import fi.kroon.vadret.library.LibraryRepository
import fi.kroon.vadret.nominatim.NominatimRepository
import fi.kroon.vadret.place.PlaceRepository
import fi.kroon.vadret.pollen.PollenRepository
import fi.kroon.vadret.radar.RadarRepository
import fi.kroon.vadret.theme.ThemeRepository
import fi.kroon.vadret.weatherforecast.WeatherForecastRepository
import fi.kroon.vadret.wind.WindRepository
import org.koin.core.module.Module
import org.koin.dsl.module

private const val PLACES_FILE = "files/sweden.csv"

val repositoryModule: Module = module {
    single<PlaceRepository> {
        PlaceRepository(
            readPlaces = { Res.readBytes(path = PLACES_FILE).decodeToString() },
            coroutineDispatcher = get()
        )
    }
    single<LibraryRepository> { LibraryRepository() }
    single<NominatimRepository> { NominatimRepository(httpClient = inject()) }
    single<PollenRepository> { PollenRepository(httpClient = inject()) }
    single<RadarRepository> {
        RadarRepository(
            httpClient = inject(),
            diskCache = get(),
            clock = get(),
            coroutineDispatcher = get()
        )
    }
    single<ForecastPreferenceRepository> { ForecastPreferenceRepository(dataStore = get()) }
    single<LanguageRepository> { LanguageRepository(dataStore = get()) }
    single<ThemeRepository> { ThemeRepository(dataStore = get()) }
    single<WeatherForecastRepository> { WeatherForecastRepository(httpClient = inject()) }
    single<WindRepository> {
        WindRepository(
            httpClient = inject(),
            clock = get()
        )
    }
}