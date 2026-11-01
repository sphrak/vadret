package se.vadretapp.vadret.pushalert.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import se.vadretapp.vadret.lifecycle.AppStartHook
import se.vadretapp.vadret.pushalert.PushAlertPlaceNamesAppStartHook
import se.vadretapp.vadret.pushalert.usecase.DefaultPublishPushAlertPlaceNamesTask
import se.vadretapp.vadret.pushalert.usecase.PublishPushAlertPlaceNamesTask

val iosPushAlertPlaceNameModule: Module = module {
    factoryOf(::DefaultPublishPushAlertPlaceNamesTask) { bind<PublishPushAlertPlaceNamesTask>() }
    single<AppStartHook>(qualifier = named(name = PUSH_ALERT_PLACE_NAMES_QUALIFIER)) {
        PushAlertPlaceNamesAppStartHook(publishPushAlertPlaceNamesTask = get())
    }
}

private const val PUSH_ALERT_PLACE_NAMES_QUALIFIER = "push-alert-place-names"