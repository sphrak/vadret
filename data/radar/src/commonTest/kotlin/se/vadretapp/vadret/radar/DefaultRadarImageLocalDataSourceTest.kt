package se.vadretapp.vadret.radar

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import se.vadretapp.vadret.radar.exception.RadarDownloadException
import se.vadretapp.vadret.radar.local.DefaultRadarImageLocalDataSource
import se.vadretapp.vadret.radar.local.RadarImageLocalDataSource
import se.vadretapp.vadret.testing.deleteRecursively
import se.vadretapp.vadret.testing.temporaryDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private const val DAY_ABOVE_LIMIT_BYTES = 134_217_729

private val today: LocalDate = LocalDate.parse(input = "2026-11-02")
private val firstImage: ByteArray = "first radar image".encodeToByteArray()
private val secondImage: ByteArray = "second radar image".encodeToByteArray()

class DefaultRadarImageLocalDataSourceTest {

    private val testScope = TestScope(StandardTestDispatcher())
    private val fileSystem: FileSystem = FileSystem.SYSTEM
    private val directory: Path = temporaryDirectory(prefix = "radar-image-store")
    private val radarImageLocalDataSource: RadarImageLocalDataSource = DefaultRadarImageLocalDataSource(
        directoryPath = directory.toString(),
        coroutineDispatcher = StandardTestDispatcher(testScope.testScheduler)
    )

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun archiveOf(files: List<ArchivedFile>): suspend (BufferedSink) -> Unit =
        { archiveSink: BufferedSink -> archiveSink.write(source = zipArchive(files = files)) }

    private fun storedNames(path: Path = directory): Set<String> =
        fileSystem.listOrNull(dir = path).orEmpty().map { storedFile: Path -> storedFile.name }.toSet()

    @Test
    fun `an image that was never stored reads as nothing`() =
        testScope.runTest {
            assertNull(
                actual = radarImageLocalDataSource.getImage(
                    date = today,
                    imageName = "radar_2611021400.tif"
                )
            )
            assertEquals(
                expected = emptySet(),
                actual = radarImageLocalDataSource.getImageNames(date = today)
            )
        }

    @Test
    fun `an archive leaves only its radar images on disk`() =
        testScope.runTest {
            radarImageLocalDataSource.setArchive(
                date = today,
                writeArchive = archiveOf(
                    files = listOf(
                        ArchivedFile(name = "radar_2611021400.tif", bytes = firstImage),
                        ArchivedFile(name = "radar_2611021405.tif", bytes = secondImage),
                        ArchivedFile(name = "palette.txt", bytes = "0 0 0".encodeToByteArray()),
                        ArchivedFile(name = "radar_2611021410.png", bytes = secondImage)
                    )
                )
            )

            assertEquals(
                expected = setOf("radar_2611021400.tif", "radar_2611021405.tif"),
                actual = radarImageLocalDataSource.getImageNames(date = today)
            )
            assertContentEquals(
                expected = secondImage,
                actual = radarImageLocalDataSource.getImage(
                    date = today,
                    imageName = "radar_2611021405.tif"
                )
            )
            assertEquals(
                expected = setOf("2026-11-02"),
                actual = storedNames()
            )
            assertEquals(
                expected = setOf("radar_2611021400.tif", "radar_2611021405.tif"),
                actual = storedNames(path = directory / "2026-11-02")
            )
        }

    @Test
    fun `an archive that unpacks to more than a day can hold is refused`() =
        testScope.runTest {
            assertFailsWith<RadarDownloadException> {
                radarImageLocalDataSource.setArchive(
                    date = today,
                    writeArchive = archiveOf(
                        files = listOf(
                            ArchivedFile(
                                name = "radar_2611021400.tif",
                                bytes = firstImage,
                                declaredSize = DAY_ABOVE_LIMIT_BYTES
                            )
                        )
                    )
                )
            }
            assertEquals(
                expected = emptySet(),
                actual = radarImageLocalDataSource.getImageNames(date = today)
            )
            assertEquals(
                expected = emptySet(),
                actual = storedNames()
            )
        }

    @Test
    fun `an archive that breaks off while downloading leaves nothing behind`() =
        testScope.runTest {
            assertFailsWith<RadarDownloadException> {
                radarImageLocalDataSource.setArchive(
                    date = today,
                    writeArchive = { archiveSink: BufferedSink ->
                        archiveSink.writeUtf8(string = "half an archive")
                        throw RadarDownloadException(message = "The connection broke off")
                    }
                )
            }
            assertEquals(
                expected = emptySet(),
                actual = storedNames()
            )
        }

    @Test
    fun `a stored image is read back`() =
        testScope.runTest {
            radarImageLocalDataSource.setImage(
                date = today,
                imageName = "radar_2611021400.tif",
                image = firstImage
            )

            assertContentEquals(
                expected = firstImage,
                actual = radarImageLocalDataSource.getImage(
                    date = today,
                    imageName = "radar_2611021400.tif"
                )
            )
        }

    @Test
    fun `only the stored day and the newest other day stay on disk`() =
        testScope.runTest {
            fileSystem.write(file = directory / "interrupted.zip") { writeUtf8(string = "half an archive") }
            listOf("2026-10-30", "2026-10-31", "2026-11-01", "2026-11-02", "2026-10-20").forEach { date: String ->
                radarImageLocalDataSource.setImage(
                    date = LocalDate.parse(input = date),
                    imageName = "radar_2611021400.tif",
                    image = firstImage
                )
            }

            assertEquals(
                expected = setOf("2026-10-20", "2026-11-02"),
                actual = storedNames()
            )
        }
}