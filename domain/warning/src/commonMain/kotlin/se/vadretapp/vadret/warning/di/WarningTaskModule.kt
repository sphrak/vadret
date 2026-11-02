package se.vadretapp.vadret.warning.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.warning.DefaultGetWarningsTask
import se.vadretapp.vadret.warning.DefaultObserveFollowedPlaceWarningsTask
import se.vadretapp.vadret.warning.DefaultObserveUnseenFollowedPlaceWarningsTask
import se.vadretapp.vadret.warning.DefaultObserveUnseenWarningsTask
import se.vadretapp.vadret.warning.GetWarningsTask
import se.vadretapp.vadret.warning.ObserveFollowedPlaceWarningsTask
import se.vadretapp.vadret.warning.ObserveUnseenFollowedPlaceWarningsTask
import se.vadretapp.vadret.warning.ObserveUnseenWarningsTask

val warningTaskModule: Module = module {
    factoryOf(::DefaultGetWarningsTask) { bind<GetWarningsTask>() }
    factoryOf(::DefaultObserveFollowedPlaceWarningsTask) { bind<ObserveFollowedPlaceWarningsTask>() }
    factoryOf(::DefaultObserveUnseenFollowedPlaceWarningsTask) { bind<ObserveUnseenFollowedPlaceWarningsTask>() }
    factoryOf(::DefaultObserveUnseenWarningsTask) { bind<ObserveUnseenWarningsTask>() }
}