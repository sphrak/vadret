package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class PushAlertPlaceNameRepositoryTest {
    private val directory: Path = NSTemporaryDirectory().toPath() / "place-names-${Uuid.random()}"

    @AfterTest
    fun tearDown() {
        FileSystem.SYSTEM.deleteRecursively(fileOrDirectory = directory)
    }

    @Test
    fun theNamesAreWrittenWhereTheNotificationServiceExtensionReadsThem() = runTest {
        FileSystem.SYSTEM.createDirectories(dir = directory)

        val saved = pushAlertPlaceNameRepository().savePlaceNames(placeNamesByCell = mapOf("u3e2" to "Malmö"))

        assertEquals(
            expected = Unit.asRight(),
            actual = saved
        )
        assertEquals(
            expected = "Malmö",
            actual = savedJson().getValue("place_names_by_cell").jsonObject.getValue("u3e2").jsonPrimitive.content
        )
        assertEquals(
            expected = listOf(directory / "push_alert_place_names.json"),
            actual = FileSystem.SYSTEM.list(dir = directory),
            message = "no half-written file is left behind"
        )
    }

    @Test
    fun newNamesReplaceTheOldOnes() = runTest {
        FileSystem.SYSTEM.createDirectories(dir = directory)
        val pushAlertPlaceNameRepository = pushAlertPlaceNameRepository()

        pushAlertPlaceNameRepository.savePlaceNames(placeNamesByCell = mapOf("u3e2" to "Malmö"))
        pushAlertPlaceNameRepository.savePlaceNames(placeNamesByCell = emptyMap())

        assertEquals(
            expected = emptyMap(),
            actual = savedJson().getValue("place_names_by_cell").jsonObject
        )
    }

    @Test
    fun aDirectoryThatCannotBeWrittenIsAStorageFailure() = runTest {
        val saved = pushAlertPlaceNameRepository().savePlaceNames(placeNamesByCell = mapOf("u3e2" to "Malmö"))

        assertEquals(
            expected = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft(),
            actual = saved
        )
    }

    private fun TestScope.pushAlertPlaceNameRepository(): PushAlertPlaceNameRepository =
        PushAlertPlaceNameRepository(
            directoryPath = directory.toString(),
            ioDispatcher = StandardTestDispatcher(scheduler = testScheduler)
        )

    private fun savedJson(): JsonObject =
        Json.parseToJsonElement(
            string = FileSystem.SYSTEM.read(file = directory / "push_alert_place_names.json") { readUtf8() }
        ).jsonObject
}