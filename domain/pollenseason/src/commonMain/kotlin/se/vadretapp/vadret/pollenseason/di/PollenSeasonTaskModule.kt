package se.vadretapp.vadret.pollenseason.di

import org.koin.core.module.Module
import org.koin.dsl.module
import se.vadretapp.vadret.pollenseason.DefaultObservePollenInSeasonTask
import se.vadretapp.vadret.pollenseason.EndOfSeasonSimulatingObservePollenInSeasonTask
import se.vadretapp.vadret.pollenseason.ObservePollenInSeasonTask

// The end of season can be simulated from the developer settings, which any build can unlock.
val pollenSeasonTaskModule: Module = module {
    factory<ObservePollenInSeasonTask> {
        EndOfSeasonSimulatingObservePollenInSeasonTask(
            observePollenInSeasonTask = DefaultObservePollenInSeasonTask(pollenRepository = get()),
            pollenPreferenceRepository = get()
        )
    }
}