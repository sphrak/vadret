package se.vadretapp.vadret.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSURLCache
import platform.Foundation.NSUserDomainMask
import se.vadretapp.vadret.aggregatedfeed.SeenWarningRepository
import se.vadretapp.vadret.aggregatedfeed.WarningsRepository
import se.vadretapp.vadret.di.model.AppDirectories
import se.vadretapp.vadret.di.model.AppInfo
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.location.LocationRepository

private const val TIMEOUT_SECONDS = 10.0
private const val MEMORY_CACHE_SIZE_BYTES: ULong = 4_194_304uL
private const val DISK_CACHE_SIZE_BYTES: ULong = 33_554_432uL
private const val HTTP_CACHE_DIRECTORY = "http_cache"

fun iosPlatformModule(appInfo: AppInfo): Module =
    module {
        single<AppInfo> { appInfo }
        single<AppDirectories> {
            AppDirectories(
                filesDirectoryPath = userDirectory(directory = NSApplicationSupportDirectory).toString(),
                cacheDirectoryPath = userDirectory(directory = NSCachesDirectory).toString()
            )
        }
        single<HttpClientEngine> { darwinEngine(cacheDirectoryPath = get<AppDirectories>().cacheDirectoryPath) }
        single<FavoriteRepository> {
            FavoriteRepository(
                databaseDirectory = get<AppDirectories>().filesDirectoryPath,
                idGenerator = get(),
                clock = get(),
                coroutineDispatcher = get()
            )
        }
        single<LocationRepository> {
            LocationRepository(
                dataStore = get(),
                coroutineDispatcher = get(),
                mainDispatcher = Dispatchers.Main
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
                databaseDirectory = get<AppDirectories>().filesDirectoryPath,
                clock = get(),
                coroutineDispatcher = get()
            )
        }
    }

private fun userDirectory(directory: ULong): Path {
    val path: Path = checkNotNull(
        value = NSSearchPathForDirectoriesInDomains(
            directory = directory,
            domainMask = NSUserDomainMask,
            expandTilde = true
        ).firstOrNull() as? String
    ) { "No user directory $directory" }.toPath()
    FileSystem.SYSTEM.createDirectories(dir = path)
    return path
}

private fun darwinEngine(cacheDirectoryPath: String): HttpClientEngine =
    Darwin.create {
        configureSession {
            timeoutIntervalForRequest = TIMEOUT_SECONDS
            URLCache = NSURLCache(
                memoryCapacity = MEMORY_CACHE_SIZE_BYTES,
                diskCapacity = DISK_CACHE_SIZE_BYTES,
                directoryURL = NSURL.fileURLWithPath(path = (cacheDirectoryPath.toPath() / HTTP_CACHE_DIRECTORY).toString())
            )
        }
    }