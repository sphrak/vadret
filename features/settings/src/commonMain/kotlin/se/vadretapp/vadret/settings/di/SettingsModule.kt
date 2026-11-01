package se.vadretapp.vadret.settings.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.settings.DefaultGetChangelogTask
import se.vadretapp.vadret.settings.DefaultGetLibrariesTask
import se.vadretapp.vadret.settings.DefaultGetSettingsTask
import se.vadretapp.vadret.settings.DefaultSetExpandOnceTask
import se.vadretapp.vadret.settings.DefaultSetHourlyForecastTask
import se.vadretapp.vadret.settings.DefaultSetLanguageTask
import se.vadretapp.vadret.settings.DefaultSetLocationModeTask
import se.vadretapp.vadret.settings.DefaultSetThemeTask
import se.vadretapp.vadret.settings.GetChangelogTask
import se.vadretapp.vadret.settings.GetLibrariesTask
import se.vadretapp.vadret.settings.GetSettingsTask
import se.vadretapp.vadret.settings.SetExpandOnceTask
import se.vadretapp.vadret.settings.SetHourlyForecastTask
import se.vadretapp.vadret.settings.SetLanguageTask
import se.vadretapp.vadret.settings.SetLocationModeTask
import se.vadretapp.vadret.settings.SetThemeTask
import se.vadretapp.vadret.settings.SettingsViewModel

val settingsModule: Module = module {
    factoryOf(::DefaultGetChangelogTask) { bind<GetChangelogTask>() }
    factoryOf(::DefaultGetLibrariesTask) { bind<GetLibrariesTask>() }
    factoryOf(::DefaultGetSettingsTask) { bind<GetSettingsTask>() }
    factoryOf(::DefaultSetExpandOnceTask) { bind<SetExpandOnceTask>() }
    factoryOf(::DefaultSetHourlyForecastTask) { bind<SetHourlyForecastTask>() }
    factoryOf(::DefaultSetLanguageTask) { bind<SetLanguageTask>() }
    factoryOf(::DefaultSetLocationModeTask) { bind<SetLocationModeTask>() }
    factoryOf(::DefaultSetThemeTask) { bind<SetThemeTask>() }
    factoryOf(::SettingsViewModel)
}