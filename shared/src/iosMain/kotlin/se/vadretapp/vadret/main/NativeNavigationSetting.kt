package se.vadretapp.vadret.main

import io.github.sphrak.either.extension.onResult
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.di.koinApplication
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.setting.SettingRepository

private const val IS_NATIVE_NAVIGATION_ENABLED_WHEN_UNREADABLE = false

suspend fun isNativeNavigationEnabled(): Boolean =
    koinApplication.koin.get<SettingRepository>().isNativeNavigationEnabled().onResult(
        onError = { failure: Failure ->
            logger.warn { "The navigation choice cannot be read, so the app keeps its own navigation: ${failure.errorCode}" }
            IS_NATIVE_NAVIGATION_ENABLED_WHEN_UNREADABLE
        },
        onSuccess = { isEnabled: Boolean -> isEnabled }
    )

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.main")