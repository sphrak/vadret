package se.vadretapp.vadret.forecastwarning.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.forecastwarning.DefaultMarkWarningSeenTask
import se.vadretapp.vadret.forecastwarning.MarkWarningSeenTask
import se.vadretapp.vadret.forecastwarning.WarningDetailViewModel
import se.vadretapp.vadret.forecastwarning.WarningStoryViewModel
import se.vadretapp.vadret.forecastwarning.WarningViewModel

val warningModule: Module = module {
    factoryOf(::DefaultMarkWarningSeenTask) { bind<MarkWarningSeenTask>() }
    factoryOf(::WarningViewModel)
    factoryOf(::WarningDetailViewModel)
    factoryOf(::WarningStoryViewModel)
}