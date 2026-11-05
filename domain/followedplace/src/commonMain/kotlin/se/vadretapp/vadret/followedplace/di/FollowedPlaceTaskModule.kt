package se.vadretapp.vadret.followedplace.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import se.vadretapp.vadret.followedplace.AddFavoriteTask
import se.vadretapp.vadret.followedplace.ChosenPlaceLookup
import se.vadretapp.vadret.followedplace.DefaultAddFavoriteTask
import se.vadretapp.vadret.followedplace.DefaultGetChosenPlaceTask
import se.vadretapp.vadret.followedplace.DefaultObserveFollowedPlacesTask
import se.vadretapp.vadret.followedplace.DefaultObserveLocationChoicesTask
import se.vadretapp.vadret.followedplace.DefaultRemoveFavoriteTask
import se.vadretapp.vadret.followedplace.DefaultSearchPlacesTask
import se.vadretapp.vadret.followedplace.DefaultSelectFavoriteTask
import se.vadretapp.vadret.followedplace.DefaultSelectPlaceTask
import se.vadretapp.vadret.followedplace.DefaultSetLocationModeTask
import se.vadretapp.vadret.followedplace.GetChosenPlaceTask
import se.vadretapp.vadret.followedplace.ObserveFollowedPlacesTask
import se.vadretapp.vadret.followedplace.ObserveLocationChoicesTask
import se.vadretapp.vadret.followedplace.RemoveFavoriteTask
import se.vadretapp.vadret.followedplace.SearchPlacesTask
import se.vadretapp.vadret.followedplace.SelectFavoriteTask
import se.vadretapp.vadret.followedplace.SelectPlaceTask
import se.vadretapp.vadret.followedplace.SetLocationModeTask

val followedPlaceTaskModule: Module = module {
    factoryOf(::ChosenPlaceLookup)
    factoryOf(::DefaultAddFavoriteTask) { bind<AddFavoriteTask>() }
    factoryOf(::DefaultGetChosenPlaceTask) { bind<GetChosenPlaceTask>() }
    factoryOf(::DefaultObserveFollowedPlacesTask) { bind<ObserveFollowedPlacesTask>() }
    factoryOf(::DefaultObserveLocationChoicesTask) { bind<ObserveLocationChoicesTask>() }
    factoryOf(::DefaultRemoveFavoriteTask) { bind<RemoveFavoriteTask>() }
    factoryOf(::DefaultSearchPlacesTask) { bind<SearchPlacesTask>() }
    factoryOf(::DefaultSelectFavoriteTask) { bind<SelectFavoriteTask>() }
    factoryOf(::DefaultSelectPlaceTask) { bind<SelectPlaceTask>() }
    factoryOf(::DefaultSetLocationModeTask) { bind<SetLocationModeTask>() }
}