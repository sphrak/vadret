package se.vadretapp.vadret.settings.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.settings.AboutViewModel
import se.vadretapp.vadret.settings.DeveloperViewModel
import se.vadretapp.vadret.settings.LabsViewModel
import se.vadretapp.vadret.settings.LicensesViewModel
import se.vadretapp.vadret.settings.PlacesViewModel
import se.vadretapp.vadret.settings.PollenSettingsViewModel
import se.vadretapp.vadret.settings.SettingsViewModel
import se.vadretapp.vadret.settings.usecase.DefaultGetChangelogTask
import se.vadretapp.vadret.settings.usecase.DefaultGetLibrariesTask
import se.vadretapp.vadret.settings.usecase.DefaultGetSettingsTask
import se.vadretapp.vadret.settings.usecase.DefaultObserveDeveloperModeTask
import se.vadretapp.vadret.settings.usecase.DefaultObserveHourlyForecastTask
import se.vadretapp.vadret.settings.usecase.DefaultObservePollenTypesTask
import se.vadretapp.vadret.settings.usecase.DefaultSetEndOfSeasonSimulatedTask
import se.vadretapp.vadret.settings.usecase.DefaultSetExpandOnceTask
import se.vadretapp.vadret.settings.usecase.DefaultSetHourlyForecastTask
import se.vadretapp.vadret.settings.usecase.DefaultSetLanguageTask
import se.vadretapp.vadret.settings.usecase.DefaultSetPollenAllergenTask
import se.vadretapp.vadret.settings.usecase.DefaultSetPollenEnabledTask
import se.vadretapp.vadret.settings.usecase.DefaultSetThemeTask
import se.vadretapp.vadret.settings.usecase.DefaultUnlockDeveloperModeTask
import se.vadretapp.vadret.settings.usecase.GetChangelogTask
import se.vadretapp.vadret.settings.usecase.GetLibrariesTask
import se.vadretapp.vadret.settings.usecase.GetSettingsTask
import se.vadretapp.vadret.settings.usecase.ObserveDeveloperModeTask
import se.vadretapp.vadret.settings.usecase.ObserveHourlyForecastTask
import se.vadretapp.vadret.settings.usecase.ObservePollenTypesTask
import se.vadretapp.vadret.settings.usecase.SetEndOfSeasonSimulatedTask
import se.vadretapp.vadret.settings.usecase.SetExpandOnceTask
import se.vadretapp.vadret.settings.usecase.SetHourlyForecastTask
import se.vadretapp.vadret.settings.usecase.SetLanguageTask
import se.vadretapp.vadret.settings.usecase.SetPollenAllergenTask
import se.vadretapp.vadret.settings.usecase.SetPollenEnabledTask
import se.vadretapp.vadret.settings.usecase.SetThemeTask
import se.vadretapp.vadret.settings.usecase.UnlockDeveloperModeTask

val settingsModule: Module = module {
    factoryOf(::DefaultGetChangelogTask) { bind<GetChangelogTask>() }
    factoryOf(::DefaultGetLibrariesTask) { bind<GetLibrariesTask>() }
    factoryOf(::DefaultObservePollenTypesTask) { bind<ObservePollenTypesTask>() }
    factoryOf(::DefaultGetSettingsTask) { bind<GetSettingsTask>() }
    factoryOf(::DefaultObserveDeveloperModeTask) { bind<ObserveDeveloperModeTask>() }
    factoryOf(::DefaultObserveHourlyForecastTask) { bind<ObserveHourlyForecastTask>() }
    factoryOf(::DefaultSetEndOfSeasonSimulatedTask) { bind<SetEndOfSeasonSimulatedTask>() }
    factoryOf(::DefaultSetExpandOnceTask) { bind<SetExpandOnceTask>() }
    factoryOf(::DefaultSetHourlyForecastTask) { bind<SetHourlyForecastTask>() }
    factoryOf(::DefaultSetLanguageTask) { bind<SetLanguageTask>() }
    factoryOf(::DefaultSetPollenAllergenTask) { bind<SetPollenAllergenTask>() }
    factoryOf(::DefaultSetPollenEnabledTask) { bind<SetPollenEnabledTask>() }
    factoryOf(::DefaultSetThemeTask) { bind<SetThemeTask>() }
    factoryOf(::DefaultUnlockDeveloperModeTask) { bind<UnlockDeveloperModeTask>() }
    factoryOf(::AboutViewModel)
    factoryOf(::DeveloperViewModel)
    factoryOf(::LabsViewModel)
    factoryOf(::LicensesViewModel)
    factoryOf(::PlacesViewModel)
    factoryOf(::PollenSettingsViewModel)
    factoryOf(::SettingsViewModel)
}