package se.vadretapp.vadret.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.main.DefaultObserveLanguageTask
import se.vadretapp.vadret.main.DefaultObserveThemeTask
import se.vadretapp.vadret.main.MainViewModel
import se.vadretapp.vadret.main.ObserveLanguageTask
import se.vadretapp.vadret.main.ObserveThemeTask

internal val mainModule: Module = module {
    factoryOf(::DefaultObserveLanguageTask) { bind<ObserveLanguageTask>() }
    factoryOf(::DefaultObserveThemeTask) { bind<ObserveThemeTask>() }
    factory<MainViewModel> {
        MainViewModel(
            observeThemeTask = get(),
            observeLanguageTask = get(),
            appStartHooks = getAll(),
            coroutineDispatcher = get()
        )
    }
}