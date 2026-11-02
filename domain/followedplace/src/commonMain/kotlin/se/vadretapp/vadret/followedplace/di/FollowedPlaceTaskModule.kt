package se.vadretapp.vadret.followedplace.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.followedplace.ChosenPlaceLookup
import se.vadretapp.vadret.followedplace.DefaultGetChosenPlaceTask
import se.vadretapp.vadret.followedplace.DefaultObserveFollowablePlacesTask
import se.vadretapp.vadret.followedplace.DefaultObserveFollowedPlacesTask
import se.vadretapp.vadret.followedplace.DefaultSetPlaceFollowedTask
import se.vadretapp.vadret.followedplace.GetChosenPlaceTask
import se.vadretapp.vadret.followedplace.ObserveFollowablePlacesTask
import se.vadretapp.vadret.followedplace.ObserveFollowedPlacesTask
import se.vadretapp.vadret.followedplace.SetPlaceFollowedTask

val followedPlaceTaskModule: Module = module {
    factoryOf(::ChosenPlaceLookup)
    factoryOf(::DefaultGetChosenPlaceTask) { bind<GetChosenPlaceTask>() }
    factoryOf(::DefaultObserveFollowablePlacesTask) { bind<ObserveFollowablePlacesTask>() }
    factoryOf(::DefaultObserveFollowedPlacesTask) { bind<ObserveFollowedPlacesTask>() }
    factoryOf(::DefaultSetPlaceFollowedTask) { bind<SetPlaceFollowedTask>() }
}