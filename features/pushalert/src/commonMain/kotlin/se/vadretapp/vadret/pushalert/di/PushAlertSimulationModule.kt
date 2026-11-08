package se.vadretapp.vadret.pushalert.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import se.vadretapp.vadret.pushalert.PushAlertSimulationSection
import se.vadretapp.vadret.pushalert.PushAlertSimulationViewModel
import se.vadretapp.vadret.pushalert.usecase.DefaultShowSimulatedPushAlertTask
import se.vadretapp.vadret.pushalert.usecase.ShowSimulatedPushAlertTask
import se.vadretapp.vadret.ui.DeveloperSettingsSection

// Only a build that binds a PushAlertNotifier can show the simulated alerts, so it adds this module itself.
val pushAlertSimulationModule: Module = module {
    factoryOf(::DefaultShowSimulatedPushAlertTask) { bind<ShowSimulatedPushAlertTask>() }
    factory<PushAlertSimulationViewModel> {
        PushAlertSimulationViewModel(
            showSimulatedPushAlertTask = get(),
            clock = get(),
            coroutineDispatcher = get()
        )
    }
    single<DeveloperSettingsSection>(qualifier = named(name = PUSH_ALERT_SIMULATION_QUALIFIER)) { PushAlertSimulationSection() }
}

private const val PUSH_ALERT_SIMULATION_QUALIFIER = "push-alert-simulation"