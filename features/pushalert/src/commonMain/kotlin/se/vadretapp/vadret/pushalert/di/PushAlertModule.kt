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
import se.vadretapp.vadret.pushalert.usecase.AddPlaceNameTask
import se.vadretapp.vadret.pushalert.usecase.DefaultAddPlaceNameTask
import se.vadretapp.vadret.pushalert.usecase.DefaultGetCurrentGeohashTask
import se.vadretapp.vadret.pushalert.usecase.DefaultGetPollenTypesTask
import se.vadretapp.vadret.pushalert.usecase.DefaultGetPushAlertGeohashesTask
import se.vadretapp.vadret.pushalert.usecase.DefaultIsOutsideSwedenTask
import se.vadretapp.vadret.pushalert.usecase.DefaultObserveFavoritesTask
import se.vadretapp.vadret.pushalert.usecase.DefaultObservePushAlertChoicesTask
import se.vadretapp.vadret.pushalert.usecase.DefaultObservePushAlertSyncPendingTask
import se.vadretapp.vadret.pushalert.usecase.DefaultOpenPushAlertTask
import se.vadretapp.vadret.pushalert.usecase.DefaultSavePushAlertChoicesTask
import se.vadretapp.vadret.pushalert.usecase.DefaultSyncPushAlertsTask
import se.vadretapp.vadret.pushalert.usecase.GetCurrentGeohashTask
import se.vadretapp.vadret.pushalert.usecase.GetPollenTypesTask
import se.vadretapp.vadret.pushalert.usecase.GetPushAlertGeohashesTask
import se.vadretapp.vadret.pushalert.usecase.IsOutsideSwedenTask
import se.vadretapp.vadret.pushalert.usecase.ObserveFavoritesTask
import se.vadretapp.vadret.pushalert.usecase.ObservePushAlertChoicesTask
import se.vadretapp.vadret.pushalert.usecase.ObservePushAlertSyncPendingTask
import se.vadretapp.vadret.pushalert.usecase.OpenPushAlertTask
import se.vadretapp.vadret.pushalert.usecase.SavePushAlertChoicesTask
import se.vadretapp.vadret.pushalert.usecase.SyncPushAlertsTask
import se.vadretapp.vadret.ui.SettingsSection

val pushAlertModule: Module = module {
    single<SyncPushAlertsTask> {
        DefaultSyncPushAlertsTask(
            pushAlertRepository = get(),
            deviceTokenSource = get(),
            notificationPermissionSource = get(),
            sealedMessageKey = get(),
            getCurrentGeohashTask = get(),
            getPushAlertGeohashesTask = get(),
            clock = get()
        )
    }
    factoryOf(::DefaultGetCurrentGeohashTask) { bind<GetCurrentGeohashTask>() }
    factoryOf(::DefaultGetPushAlertGeohashesTask) { bind<GetPushAlertGeohashesTask>() }
    factoryOf(::DefaultIsOutsideSwedenTask) { bind<IsOutsideSwedenTask>() }
    factoryOf(::DefaultObservePushAlertChoicesTask) { bind<ObservePushAlertChoicesTask>() }
    factoryOf(::DefaultObservePushAlertSyncPendingTask) { bind<ObservePushAlertSyncPendingTask>() }
    factoryOf(::DefaultObserveFavoritesTask) { bind<ObserveFavoritesTask>() }
    factoryOf(::DefaultSavePushAlertChoicesTask) { bind<SavePushAlertChoicesTask>() }
    factoryOf(::DefaultGetPollenTypesTask) { bind<GetPollenTypesTask>() }
    factoryOf(::DefaultAddPlaceNameTask) { bind<AddPlaceNameTask>() }
    factoryOf(::DefaultOpenPushAlertTask) { bind<OpenPushAlertTask>() }
    factory<PushAlertsViewModel> {
        PushAlertsViewModel(
            observePushAlertChoicesTask = get(),
            observePushAlertSyncPendingTask = get(),
            observeFavoritesTask = get(),
            savePushAlertChoicesTask = get(),
            syncPushAlertsTask = get(),
            getPollenTypesTask = get(),
            isOutsideSwedenTask = get(),
            coroutineDispatcher = get()
        )
    }
    single<SettingsSection>(qualifier = named(name = PUSH_ALERTS_QUALIFIER)) { PushAlertsSettingsSection() }
    single<AppStartHook>(qualifier = named(name = PUSH_ALERTS_QUALIFIER)) { PushAlertsAppStartHook(syncPushAlertsTask = get()) }
}

private const val PUSH_ALERTS_QUALIFIER = "push-alerts"