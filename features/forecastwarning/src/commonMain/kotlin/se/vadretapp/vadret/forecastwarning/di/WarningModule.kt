package se.vadretapp.vadret.forecastwarning.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import se.vadretapp.vadret.forecastwarning.DefaultMarkWarningSeenTask
import se.vadretapp.vadret.forecastwarning.MarkWarningSeenTask
import se.vadretapp.vadret.forecastwarning.SIMULATED_WARNING_STORY_QUALIFIER
import se.vadretapp.vadret.forecastwarning.WarningDetailViewModel
import se.vadretapp.vadret.forecastwarning.WarningStorySimulationViewModel
import se.vadretapp.vadret.forecastwarning.WarningStoryViewModel
import se.vadretapp.vadret.forecastwarning.WarningViewModel
import se.vadretapp.vadret.forecastwarning.simulation.SimulatedGetAuroraHoursTask
import se.vadretapp.vadret.forecastwarning.simulation.SimulatedGetWarningsTask
import se.vadretapp.vadret.forecastwarning.simulation.SimulatedMarkWarningSeenTask

val warningModule: Module = module {
    factoryOf(::DefaultMarkWarningSeenTask) { bind<MarkWarningSeenTask>() }
    factoryOf(::WarningViewModel)
    factoryOf(::WarningDetailViewModel)
    factoryOf(::WarningStoryViewModel)
    factory<WarningStoryViewModel>(qualifier = named(name = SIMULATED_WARNING_STORY_QUALIFIER)) {
        WarningStoryViewModel(
            getWarningsTask = SimulatedGetWarningsTask(clock = get()),
            getAuroraHoursTask = SimulatedGetAuroraHoursTask(),
            markWarningSeenTask = SimulatedMarkWarningSeenTask(),
            clock = get(),
            coroutineDispatcher = get()
        )
    }
    factory<WarningStorySimulationViewModel> {
        WarningStorySimulationViewModel(
            getWarningsTask = SimulatedGetWarningsTask(clock = get()),
            clock = get(),
            coroutineDispatcher = get()
        )
    }
}