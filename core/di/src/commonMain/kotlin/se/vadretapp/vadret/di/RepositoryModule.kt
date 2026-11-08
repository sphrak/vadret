package se.vadretapp.vadret.di

import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import se.vadretapp.vadret.developermode.DeveloperModeRepository
import se.vadretapp.vadret.di.model.AppDirectories
import se.vadretapp.vadret.di.resources.Res
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository
import se.vadretapp.vadret.library.LibraryRepository
import se.vadretapp.vadret.nominatim.NominatimRepository
import se.vadretapp.vadret.place.PlaceRepository
import se.vadretapp.vadret.pollen.PollenRepository
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository
import se.vadretapp.vadret.radar.RadarRepository
import se.vadretapp.vadret.weatherforecast.WeatherForecastRepository
import se.vadretapp.vadret.wind.WindRepository

private const val PLACES_FILE = "files/sweden.csv"
private const val RADAR_IMAGE_DIRECTORY = "radar_images"

val repositoryModule: Module = module {
    single<PlaceRepository> {
        PlaceRepository(
            readPlaces = { Res.readBytes(path = PLACES_FILE).decodeToString() },
            coroutineDispatcher = get()
        )
    }
    single<LibraryRepository> { LibraryRepository() }
    single<NominatimRepository> { NominatimRepository(httpClient = inject()) }
    single<PollenRepository> {
        PollenRepository(
            httpClient = inject(),
            diskCache = get(),
            clock = get(),
            coroutineDispatcher = get()
        )
    }
    single<PollenPreferenceRepository> { PollenPreferenceRepository(dataStore = get()) }
    single<RadarRepository> {
        RadarRepository(
            httpClient = inject(),
            diskCache = get(),
            imageDirectoryPath = (get<AppDirectories>().cacheDirectoryPath.toPath() / RADAR_IMAGE_DIRECTORY).toString(),
            clock = get(),
            coroutineDispatcher = get()
        )
    }
    single<DeveloperModeRepository> { DeveloperModeRepository(dataStore = get()) }
    single<ForecastPreferenceRepository> { ForecastPreferenceRepository(dataStore = get()) }
    single<WeatherForecastRepository> {
        WeatherForecastRepository(
            httpClient = inject(),
            diskCache = get(),
            clock = get(),
            coroutineDispatcher = get()
        )
    }
    single<WindRepository> {
        WindRepository(
            httpClient = inject(),
            diskCache = get(),
            clock = get(),
            coroutineDispatcher = get()
        )
    }
}