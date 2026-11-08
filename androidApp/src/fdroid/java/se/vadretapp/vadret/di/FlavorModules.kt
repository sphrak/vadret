package se.vadretapp.vadret.di

import android.content.Context
import org.koin.core.module.Module
import org.koin.dsl.module
import se.vadretapp.vadret.warning.NoInstallationGetForecastWarningRequestTask
import se.vadretapp.vadret.warning.usecase.GetForecastWarningRequestTask

internal fun flavorModules(context: Context): List<Module> = listOf(
    module {
        factory<GetForecastWarningRequestTask> { NoInstallationGetForecastWarningRequestTask() }
    }
)