package se.vadretapp.vadret

import android.app.Application
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import se.vadretapp.vadret.di.appModules
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.logging.logcatSink

private const val LOGCAT_SINK_NAME = "logcat"

abstract class BaseApplication : Application() {

    internal val koinApplication: KoinApplication by lazy {
        koinApplication {
            modules(modules = appModules(context = this@BaseApplication))
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            LoggerFactory.register(
                name = LOGCAT_SINK_NAME,
                sink = logcatSink(minimumLevel = LogLevel.DEBUG)
            )
        }
    }
}