package se.vadretapp.vadret.setting.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.setting.usecase.DefaultGetHourFormatTask
import se.vadretapp.vadret.setting.usecase.DefaultGetShownLanguageTask
import se.vadretapp.vadret.setting.usecase.DefaultObserveHourFormatTask
import se.vadretapp.vadret.setting.usecase.DefaultObserveShownLanguageTask
import se.vadretapp.vadret.setting.usecase.GetHourFormatTask
import se.vadretapp.vadret.setting.usecase.GetShownLanguageTask
import se.vadretapp.vadret.setting.usecase.ObserveHourFormatTask
import se.vadretapp.vadret.setting.usecase.ObserveShownLanguageTask

val settingTaskModule: Module = module {
    factoryOf(::DefaultGetHourFormatTask) { bind<GetHourFormatTask>() }
    factoryOf(::DefaultGetShownLanguageTask) { bind<GetShownLanguageTask>() }
    factoryOf(::DefaultObserveHourFormatTask) { bind<ObserveHourFormatTask>() }
    factoryOf(::DefaultObserveShownLanguageTask) { bind<ObserveShownLanguageTask>() }
}