package se.vadretapp.vadret.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import se.vadretapp.vadret.common.DiskCache
import se.vadretapp.vadret.di.model.AppDirectories
import se.vadretapp.vadret.idgenerator.IdGenerator
import kotlin.time.Clock

private const val SETTINGS_FILE_NAME = "settings.preferences_pb"
private const val DISK_CACHE_DIRECTORY = "disk_cache"

val coreModule: Module = module {
    factory<Clock> { Clock.System }
    factory<CoroutineDispatcher> { Dispatchers.IO }
    single<DataStore<Preferences>> {
        val settingsFile: Path = get<AppDirectories>().filesDirectoryPath.toPath() / SETTINGS_FILE_NAME
        PreferenceDataStoreFactory.createWithPath(produceFile = { settingsFile })
    }
    single<IdGenerator> { IdGenerator() }
    single<DiskCache> {
        DiskCache(directoryPath = (get<AppDirectories>().cacheDirectoryPath.toPath() / DISK_CACHE_DIRECTORY).toString())
    }
}