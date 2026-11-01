package fi.kroon.vadret.di

import fi.kroon.vadret.main.DefaultObserveLanguageTask
import fi.kroon.vadret.main.DefaultObserveThemeTask
import fi.kroon.vadret.main.MainViewModel
import fi.kroon.vadret.main.ObserveLanguageTask
import fi.kroon.vadret.main.ObserveThemeTask
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

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