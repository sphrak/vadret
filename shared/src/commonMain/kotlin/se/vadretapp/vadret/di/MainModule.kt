package se.vadretapp.vadret.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.main.DefaultObserveHourFormatTask
import se.vadretapp.vadret.main.DefaultObserveLanguageTask
import se.vadretapp.vadret.main.DefaultObserveThemeTask
import se.vadretapp.vadret.main.MainViewModel
import se.vadretapp.vadret.main.ObserveHourFormatTask
import se.vadretapp.vadret.main.ObserveLanguageTask
import se.vadretapp.vadret.main.ObserveThemeTask
import se.vadretapp.vadret.main.OpenedWarningReferences

internal val mainModule: Module = module {
    factoryOf(::DefaultObserveHourFormatTask) { bind<ObserveHourFormatTask>() }
    factoryOf(::DefaultObserveLanguageTask) { bind<ObserveLanguageTask>() }
    factoryOf(::DefaultObserveThemeTask) { bind<ObserveThemeTask>() }
    single<OpenedWarningReferences> { OpenedWarningReferences() }
    factory<MainViewModel> {
        MainViewModel(
            observeThemeTask = get(),
            observeLanguageTask = get(),
            observeHourFormatTask = get(),
            appStartHooks = getAll(),
            openedWarningReferences = get(),
            getReferencedWarningIdentifiersTask = get(),
            coroutineDispatcher = get()
        )
    }
}