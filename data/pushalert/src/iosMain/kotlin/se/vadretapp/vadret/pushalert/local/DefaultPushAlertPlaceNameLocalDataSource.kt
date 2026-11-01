package se.vadretapp.vadret.pushalert.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath
import se.vadretapp.vadret.common.defaultJson
import se.vadretapp.vadret.pushalert.error.StorageException
import se.vadretapp.vadret.pushalert.model.PushAlertPlaceNamesEntity

internal class DefaultPushAlertPlaceNameLocalDataSource(
    private val directoryPath: String,
    private val ioDispatcher: CoroutineDispatcher
) : PushAlertPlaceNameLocalDataSource {
    // The extension may read while the app writes, so it only ever sees the whole old file or the whole new one.
    override suspend fun save(pushAlertPlaceNamesEntity: PushAlertPlaceNamesEntity) {
        withContext(context = ioDispatcher) {
            val path = directoryPath.toPath() / PLACE_NAMES_FILE_NAME
            val temporaryPath = directoryPath.toPath() / TEMPORARY_PLACE_NAMES_FILE_NAME
            val json = defaultJson.encodeToString(
                serializer = PushAlertPlaceNamesEntity.serializer(),
                value = pushAlertPlaceNamesEntity
            )
            try {
                FileSystem.SYSTEM.write(file = temporaryPath) { writeUtf8(string = json) }
                FileSystem.SYSTEM.atomicMove(
                    source = temporaryPath,
                    target = path
                )
            } catch (exception: IOException) {
                throw StorageException(message = "The push alert place names cannot be written: ${exception.message}")
            }
        }
    }
}

// The Notification Service Extension reads this file by name.
private const val PLACE_NAMES_FILE_NAME = "push_alert_place_names.json"
private const val TEMPORARY_PLACE_NAMES_FILE_NAME = "push_alert_place_names.json.tmp"