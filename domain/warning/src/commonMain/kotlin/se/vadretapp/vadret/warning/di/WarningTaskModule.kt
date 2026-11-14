package se.vadretapp.vadret.warning.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.warning.usecase.DefaultGetAuroraHoursTask
import se.vadretapp.vadret.warning.usecase.DefaultGetReferencedWarningIdentifiersTask
import se.vadretapp.vadret.warning.usecase.DefaultGetWarningsTask
import se.vadretapp.vadret.warning.usecase.DefaultObserveFollowedPlaceWarningsTask
import se.vadretapp.vadret.warning.usecase.DefaultObserveUnseenWarningsTask
import se.vadretapp.vadret.warning.usecase.GetAuroraHoursTask
import se.vadretapp.vadret.warning.usecase.GetReferencedWarningIdentifiersTask
import se.vadretapp.vadret.warning.usecase.GetWarningsTask
import se.vadretapp.vadret.warning.usecase.ObserveFollowedPlaceWarningsTask
import se.vadretapp.vadret.warning.usecase.ObserveUnseenWarningsTask

val warningTaskModule: Module = module {
    factoryOf(::DefaultGetWarningsTask) { bind<GetWarningsTask>() }
    factoryOf(::DefaultGetReferencedWarningIdentifiersTask) { bind<GetReferencedWarningIdentifiersTask>() }
    factoryOf(::DefaultGetAuroraHoursTask) { bind<GetAuroraHoursTask>() }
    factoryOf(::DefaultObserveFollowedPlaceWarningsTask) { bind<ObserveFollowedPlaceWarningsTask>() }
    factoryOf(::DefaultObserveUnseenWarningsTask) { bind<ObserveUnseenWarningsTask>() }
}