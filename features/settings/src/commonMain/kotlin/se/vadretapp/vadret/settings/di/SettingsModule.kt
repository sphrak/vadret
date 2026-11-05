package se.vadretapp.vadret.settings.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.settings.AboutViewModel
import se.vadretapp.vadret.settings.DefaultGetChangelogTask
import se.vadretapp.vadret.settings.DefaultGetLibrariesTask
import se.vadretapp.vadret.settings.DefaultGetPollenTypesTask
import se.vadretapp.vadret.settings.DefaultGetSettingsTask
import se.vadretapp.vadret.settings.DefaultObserveDeveloperModeTask
import se.vadretapp.vadret.settings.DefaultSetEndOfSeasonSimulatedTask
import se.vadretapp.vadret.settings.DefaultSetExpandOnceTask
import se.vadretapp.vadret.settings.DefaultSetHourlyForecastTask
import se.vadretapp.vadret.settings.DefaultSetLanguageTask
import se.vadretapp.vadret.settings.DefaultSetPollenAllergenTask
import se.vadretapp.vadret.settings.DefaultSetPollenEnabledTask
import se.vadretapp.vadret.settings.DefaultSetThemeTask
import se.vadretapp.vadret.settings.DefaultUnlockDeveloperModeTask
import se.vadretapp.vadret.settings.DeveloperViewModel
import se.vadretapp.vadret.settings.GetChangelogTask
import se.vadretapp.vadret.settings.GetLibrariesTask
import se.vadretapp.vadret.settings.GetPollenTypesTask
import se.vadretapp.vadret.settings.GetSettingsTask
import se.vadretapp.vadret.settings.LicensesViewModel
import se.vadretapp.vadret.settings.ObserveDeveloperModeTask
import se.vadretapp.vadret.settings.PlacesViewModel
import se.vadretapp.vadret.settings.PollenSettingsViewModel
import se.vadretapp.vadret.settings.SetEndOfSeasonSimulatedTask
import se.vadretapp.vadret.settings.SetExpandOnceTask
import se.vadretapp.vadret.settings.SetHourlyForecastTask
import se.vadretapp.vadret.settings.SetLanguageTask
import se.vadretapp.vadret.settings.SetPollenAllergenTask
import se.vadretapp.vadret.settings.SetPollenEnabledTask
import se.vadretapp.vadret.settings.SetThemeTask
import se.vadretapp.vadret.settings.SettingsViewModel
import se.vadretapp.vadret.settings.UnlockDeveloperModeTask

val settingsModule: Module = module {
    factoryOf(::DefaultGetChangelogTask) { bind<GetChangelogTask>() }
    factoryOf(::DefaultGetLibrariesTask) { bind<GetLibrariesTask>() }
    factoryOf(::DefaultGetPollenTypesTask) { bind<GetPollenTypesTask>() }
    factoryOf(::DefaultGetSettingsTask) { bind<GetSettingsTask>() }
    factoryOf(::DefaultObserveDeveloperModeTask) { bind<ObserveDeveloperModeTask>() }
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
    factoryOf(::LicensesViewModel)
    factoryOf(::PlacesViewModel)
    factoryOf(::PollenSettingsViewModel)
    factoryOf(::SettingsViewModel)
}