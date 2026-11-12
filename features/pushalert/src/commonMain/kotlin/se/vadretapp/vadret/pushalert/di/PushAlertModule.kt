package se.vadretapp.vadret.pushalert.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import se.vadretapp.vadret.lifecycle.AppStartHook
import se.vadretapp.vadret.pushalert.PushAlertsAppStartHook
import se.vadretapp.vadret.pushalert.PushAlertsSettingsSection
import se.vadretapp.vadret.pushalert.PushAlertsViewModel
import se.vadretapp.vadret.pushalert.usecase.DefaultGetCurrentGeohashTask
import se.vadretapp.vadret.pushalert.usecase.DefaultGetForecastWarningRequestTask
import se.vadretapp.vadret.pushalert.usecase.DefaultGetPushAlertPlaceNamesTask
import se.vadretapp.vadret.pushalert.usecase.DefaultIsOutsideSwedenTask
import se.vadretapp.vadret.pushalert.usecase.DefaultObserveAuroraEnabledTask
import se.vadretapp.vadret.pushalert.usecase.DefaultObservePollenAllergensTask
import se.vadretapp.vadret.pushalert.usecase.DefaultObservePushAlertChoicesTask
import se.vadretapp.vadret.pushalert.usecase.DefaultObservePushAlertPlacesTask
import se.vadretapp.vadret.pushalert.usecase.DefaultObservePushAlertSyncPendingTask
import se.vadretapp.vadret.pushalert.usecase.DefaultOpenPushAlertTask
import se.vadretapp.vadret.pushalert.usecase.DefaultSavePushAlertChoicesTask
import se.vadretapp.vadret.pushalert.usecase.DefaultSyncPushAlertsTask
import se.vadretapp.vadret.pushalert.usecase.GetCurrentGeohashTask
import se.vadretapp.vadret.pushalert.usecase.GetPushAlertPlaceNamesTask
import se.vadretapp.vadret.pushalert.usecase.IsOutsideSwedenTask
import se.vadretapp.vadret.pushalert.usecase.ObserveAuroraEnabledTask
import se.vadretapp.vadret.pushalert.usecase.ObservePollenAllergensTask
import se.vadretapp.vadret.pushalert.usecase.ObservePushAlertChoicesTask
import se.vadretapp.vadret.pushalert.usecase.ObservePushAlertPlacesTask
import se.vadretapp.vadret.pushalert.usecase.ObservePushAlertSyncPendingTask
import se.vadretapp.vadret.pushalert.usecase.OpenPushAlertTask
import se.vadretapp.vadret.pushalert.usecase.SavePushAlertChoicesTask
import se.vadretapp.vadret.pushalert.usecase.SyncPushAlertsTask
import se.vadretapp.vadret.ui.SettingsSection
import se.vadretapp.vadret.warning.usecase.GetForecastWarningRequestTask

val pushAlertModule: Module = module {
    single<SyncPushAlertsTask> {
        DefaultSyncPushAlertsTask(
            pushAlertRepository = get(),
            pollenPreferenceRepository = get(),
            forecastPreferenceRepository = get(),
            getHourFormatTask = get(),
            getShownLanguageTask = get(),
            deviceTokenSource = get(),
            notificationPermissionSource = get(),
            sealedMessageKey = get(),
            getCurrentGeohashTask = get(),
            getPushAlertPlaceNamesTask = get(),
            clock = get()
        )
    }
    factoryOf(::DefaultGetCurrentGeohashTask) { bind<GetCurrentGeohashTask>() }
    factoryOf(::DefaultGetPushAlertPlaceNamesTask) { bind<GetPushAlertPlaceNamesTask>() }
    factoryOf(::DefaultGetForecastWarningRequestTask) { bind<GetForecastWarningRequestTask>() }
    factoryOf(::DefaultIsOutsideSwedenTask) { bind<IsOutsideSwedenTask>() }
    factoryOf(::DefaultObservePushAlertChoicesTask) { bind<ObservePushAlertChoicesTask>() }
    factoryOf(::DefaultObservePushAlertPlacesTask) { bind<ObservePushAlertPlacesTask>() }
    factoryOf(::DefaultObservePushAlertSyncPendingTask) { bind<ObservePushAlertSyncPendingTask>() }
    factoryOf(::DefaultSavePushAlertChoicesTask) { bind<SavePushAlertChoicesTask>() }
    factoryOf(::DefaultObserveAuroraEnabledTask) { bind<ObserveAuroraEnabledTask>() }
    factoryOf(::DefaultObservePollenAllergensTask) { bind<ObservePollenAllergensTask>() }
    factoryOf(::DefaultOpenPushAlertTask) { bind<OpenPushAlertTask>() }
    factory<PushAlertsViewModel> {
        PushAlertsViewModel(
            observePushAlertChoicesTask = get(),
            observePushAlertPlacesTask = get(),
            observePushAlertSyncPendingTask = get(),
            savePushAlertChoicesTask = get(),
            syncPushAlertsTask = get(),
            observePollenAllergensTask = get(),
            observeAuroraEnabledTask = get(),
            isOutsideSwedenTask = get(),
            coroutineDispatcher = get()
        )
    }
    single<SettingsSection>(qualifier = named(name = PUSH_ALERTS_QUALIFIER)) { PushAlertsSettingsSection() }
    single<AppStartHook>(qualifier = named(name = PUSH_ALERTS_QUALIFIER)) {
        PushAlertsAppStartHook(
            syncPushAlertsTask = get(),
            observeFollowedPlacesTask = get(),
            observePollenAllergensTask = get(),
            observeShownLanguageTask = get(),
            observeHourFormatTask = get(),
            observeAuroraEnabledTask = get()
        )
    }
}

private const val PUSH_ALERTS_QUALIFIER = "push-alerts"