package se.vadretapp.vadret.di

import android.content.Context
import android.location.LocationManager
import android.os.Looper
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.http.HttpHeaders
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.Module
import org.koin.dsl.module
import se.vadretapp.vadret.aggregatedfeed.SeenWarningRepository
import se.vadretapp.vadret.aggregatedfeed.WarningsRepository
import se.vadretapp.vadret.di.model.AppDirectories
import se.vadretapp.vadret.di.model.AppInfo
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

private const val CONNECTION_TIMEOUT_MILLIS = 10_000L
private const val READ_TIMEOUT_MILLIS = 10_000L
private const val WRITE_TIMEOUT_MILLIS = 10_000L
private const val CACHE_SIZE_BYTES: Long = 33_554_432L
private const val CACHE_DIRECTORY = "okhttp_cache"

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.di")

fun androidPlatformModule(
    context: Context,
    appInfo: AppInfo
): Module =
    module {
        single<Context> { context }
        single<AppInfo> { appInfo }
        single<AppDirectories> {
            AppDirectories(
                filesDirectoryPath = context.filesDir.absolutePath,
                cacheDirectoryPath = context.cacheDir.absolutePath
            )
        }
        single<HttpClientEngine> {
            okHttpEngine(
                context = context,
                appInfo = appInfo
            )
        }
        single<FavoriteRepository> {
            FavoriteRepository(
                context = context,
                idGenerator = get(),
                clock = get()
            )
        }
        single<LocationRepository> {
            LocationRepository(
                locationManager = context.getSystemService(LocationManager::class.java),
                dataStore = get(),
                coroutineDispatcher = get()
            )
        }
        single<WarningsRepository> {
            WarningsRepository(
                httpClient = inject(),
                diskCache = get(),
                clock = get(),
                coroutineDispatcher = get()
            )
        }
        single<SeenWarningRepository> {
            SeenWarningRepository(
                context = context,
                clock = get()
            )
        }
    }

private fun okHttpEngine(
    context: Context,
    appInfo: AppInfo
): HttpClientEngine {
    check(Looper.myLooper() != Looper.getMainLooper()) { "The HTTP client must not be built on the main thread" }
    return OkHttp.create {
        preconfigured = OkHttpClient
            .Builder()
            .connectTimeout(CONNECTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .writeTimeout(WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .cache(
                Cache(
                    directory = File(context.cacheDir, CACHE_DIRECTORY),
                    maxSize = CACHE_SIZE_BYTES
                )
            )
            .addInterceptor(httpLoggingInterceptor(isDeveloperBuild = appInfo.isDeveloperBuild))
            .build()
    }
}

private fun httpLoggingInterceptor(isDeveloperBuild: Boolean): HttpLoggingInterceptor =
    HttpLoggingInterceptor(
        logger = HttpLoggingInterceptor.Logger { message: String -> logger.debug { message } }
    ).apply {
        redactHeader(name = HttpHeaders.Authorization)
        level = if (isDeveloperBuild) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }