package se.vadretapp.vadret.radar.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.openZip
import se.vadretapp.vadret.radar.exception.RadarDownloadException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val TEMPORARY_FILE_SUFFIX = ".tmp"
private const val ARCHIVE_FILE_SUFFIX = ".zip"
private const val ARCHIVE_ROOT = "/"
private const val MAXIMUM_UNPACKED_BYTES: Long = 134_217_728L

private val imageNamePattern = Regex(pattern = """radar_\d{10}\.tif""")

internal class DefaultRadarImageLocalDataSource(
    directoryPath: String,
    private val coroutineDispatcher: CoroutineDispatcher
) : RadarImageLocalDataSource {

    private val fileSystem: FileSystem = FileSystem.SYSTEM
    private val directory: Path = directoryPath.toPath()

    override suspend fun getImage(
        date: LocalDate,
        imageName: String
    ): ByteArray? =
        withContext(context = coroutineDispatcher) {
            val imageFile: Path = dateDirectory(date = date) / imageName
            if (fileSystem.exists(path = imageFile)) {
                fileSystem.read(file = imageFile) { readByteArray() }
            } else {
                null
            }
        }

    override suspend fun getImageNames(date: LocalDate): Set<String> =
        withContext(context = coroutineDispatcher) {
            fileSystem.listOrNull(dir = dateDirectory(date = date))
                .orEmpty()
                .map { imageFile: Path -> imageFile.name }
                .filter { imageName: String -> imageNamePattern.matches(input = imageName) }
                .toSet()
        }

    override suspend fun setImage(
        date: LocalDate,
        imageName: String,
        image: ByteArray
    ) {
        withContext(context = coroutineDispatcher) {
            deleteDatesOtherThan(date = date)
            writeImage(
                date = date,
                imageName = imageName,
                image = image
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun setArchive(
        date: LocalDate,
        writeArchive: suspend (BufferedSink) -> Unit
    ) {
        withContext(context = coroutineDispatcher) {
            deleteDatesOtherThan(date = date)
            val archiveFile: Path = directory / "${Uuid.random()}$ARCHIVE_FILE_SUFFIX"
            fileSystem.createDirectories(dir = directory)
            try {
                fileSystem.write(file = archiveFile) { writeArchive(this) }
                unpack(
                    archiveFile = archiveFile,
                    date = date
                )
            } finally {
                fileSystem.delete(path = archiveFile)
            }
        }
    }

    // The archive comes from SMHI, so only radar images are taken out of it and only as much as a day can hold.
    private fun unpack(
        archiveFile: Path,
        date: LocalDate
    ) {
        val archiveFileSystem: FileSystem = fileSystem.openZip(zipPath = archiveFile)
        val imageFiles: List<Path> = archiveFileSystem
            .list(dir = ARCHIVE_ROOT.toPath())
            .filter { imageFile: Path -> imageNamePattern.matches(input = imageFile.name) }
        val unpackedBytes: Long = imageFiles.sumOf { imageFile: Path ->
            archiveFileSystem.metadata(path = imageFile).size
                ?: throw RadarDownloadException(message = "${imageFile.name} in the $date radar archive has no size")
        }
        if (unpackedBytes > MAXIMUM_UNPACKED_BYTES) {
            throw RadarDownloadException(message = "The $date radar archive unpacks to $unpackedBytes bytes")
        }
        imageFiles.forEach { imageFile: Path ->
            writeImage(
                date = date,
                imageName = imageFile.name,
                image = archiveFileSystem.read(file = imageFile) { readByteArray() }
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun writeImage(
        date: LocalDate,
        imageName: String,
        image: ByteArray
    ) {
        val dateDirectory: Path = dateDirectory(date = date)
        fileSystem.createDirectories(dir = dateDirectory)
        val temporaryFile: Path = dateDirectory / "$imageName.${Uuid.random()}$TEMPORARY_FILE_SUFFIX"
        try {
            fileSystem.write(file = temporaryFile) { write(source = image) }
            fileSystem.atomicMove(
                source = temporaryFile,
                target = dateDirectory / imageName
            )
        } finally {
            fileSystem.delete(path = temporaryFile)
        }
    }

    // At most two days stay on disk, which keeps the images to about 15 MB.
    private fun deleteDatesOtherThan(date: LocalDate) {
        val storedFiles: List<Path> = fileSystem.listOrNull(dir = directory).orEmpty()
        val keptDateDirectory: Path = dateDirectory(date = date)
        val newestOtherDateDirectory: Path? = storedFiles
            .filter { storedFile: Path -> storedFile != keptDateDirectory && storedFile.isDateDirectory() }
            .maxByOrNull { storedFile: Path -> storedFile.name }
        storedFiles
            .filter { storedFile: Path -> storedFile != keptDateDirectory && storedFile != newestOtherDateDirectory }
            .forEach { storedFile: Path -> fileSystem.deleteRecursively(fileOrDirectory = storedFile) }
    }

    private fun Path.isDateDirectory(): Boolean = LocalDate.Formats.ISO.parseOrNull(input = name) != null

    private fun dateDirectory(date: LocalDate): Path = directory / date.toString()
}