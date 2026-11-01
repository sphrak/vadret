package fi.kroon.vadret.pushalert.di

import fi.kroon.vadret.lifecycle.AppStartHook
import fi.kroon.vadret.pushalert.PushAlertsAppStartHook
import fi.kroon.vadret.pushalert.PushAlertsSettingsSection
import fi.kroon.vadret.pushalert.PushAlertsViewModel
import fi.kroon.vadret.pushalert.usecase.DefaultGetCurrentGeohashTask
import fi.kroon.vadret.pushalert.usecase.DefaultGetPollenTypesTask
import fi.kroon.vadret.pushalert.usecase.DefaultIsOutsideSwedenTask
import fi.kroon.vadret.pushalert.usecase.DefaultObservePushAlertChoicesTask
import fi.kroon.vadret.pushalert.usecase.DefaultObservePushAlertSyncPendingTask
import fi.kroon.vadret.pushalert.usecase.DefaultOpenPushAlertTask
import fi.kroon.vadret.pushalert.usecase.DefaultSavePushAlertChoicesTask
import fi.kroon.vadret.pushalert.usecase.DefaultSyncPushAlertsTask
import fi.kroon.vadret.pushalert.usecase.GetCurrentGeohashTask
import fi.kroon.vadret.pushalert.usecase.GetPollenTypesTask
import fi.kroon.vadret.pushalert.usecase.IsOutsideSwedenTask
import fi.kroon.vadret.pushalert.usecase.ObservePushAlertChoicesTask
import fi.kroon.vadret.pushalert.usecase.ObservePushAlertSyncPendingTask
import fi.kroon.vadret.pushalert.usecase.OpenPushAlertTask
import fi.kroon.vadret.pushalert.usecase.SavePushAlertChoicesTask
import fi.kroon.vadret.pushalert.usecase.SyncPushAlertsTask
import fi.kroon.vadret.ui.SettingsSection
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val pushAlertModule: Module = module {
    single<SyncPushAlertsTask> {
        DefaultSyncPushAlertsTask(
            pushAlertRepository = get(),
            deviceTokenSource = get(),
            notificationPermissionSource = get(),
            sealedMessageKey = get(),
            getCurrentGeohashTask = get(),
            clock = get()
        )
    }
    factoryOf(::DefaultGetCurrentGeohashTask) { bind<GetCurrentGeohashTask>() }
    factoryOf(::DefaultIsOutsideSwedenTask) { bind<IsOutsideSwedenTask>() }
    factoryOf(::DefaultObservePushAlertChoicesTask) { bind<ObservePushAlertChoicesTask>() }
    factoryOf(::DefaultObservePushAlertSyncPendingTask) { bind<ObservePushAlertSyncPendingTask>() }
    factoryOf(::DefaultSavePushAlertChoicesTask) { bind<SavePushAlertChoicesTask>() }
    factoryOf(::DefaultGetPollenTypesTask) { bind<GetPollenTypesTask>() }
    factoryOf(::DefaultOpenPushAlertTask) { bind<OpenPushAlertTask>() }
    factory<PushAlertsViewModel> {
        PushAlertsViewModel(
            observePushAlertChoicesTask = get(),
            observePushAlertSyncPendingTask = get(),
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