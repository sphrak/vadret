package se.vadretapp.vadret.common

import okio.FileSystem
import okio.IOException
import okio.Path
import se.vadretapp.vadret.testing.deleteRecursively
import se.vadretapp.vadret.testing.temporaryDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultDiskCacheTest {

    private val fileSystem: FileSystem = FileSystem.SYSTEM
    private val root: Path = temporaryDirectory(prefix = "disk-cache-test")
    private val directory: Path = root / "cache"
    private val diskCache: DiskCache = DiskCache(directoryPath = directory.toString())

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun fileNames(): List<String> = fileSystem.list(dir = directory).map { path: Path -> path.name }

    @Test
    fun `a missing entry reads as null`() {
        assertNull(actual = diskCache.read(key = "radar.json"))
    }

    @Test
    fun `a written entry reads back and creates the directory`() {
        diskCache.write(
            key = "radar.json",
            value = "{}"
        )

        assertEquals(expected = "{}", actual = diskCache.read(key = "radar.json"))
        assertTrue(actual = fileSystem.metadata(path = directory).isDirectory)
    }

    @Test
    fun `a second write replaces the first and leaves no temporary file`() {
        diskCache.write(
            key = "radar.json",
            value = "first"
        )
        diskCache.write(
            key = "radar.json",
            value = "second"
        )

        assertEquals(expected = "second", actual = diskCache.read(key = "radar.json"))
        assertEquals(expected = listOf("radar.json"), actual = fileNames())
    }

    @Test
    fun `a write that cannot replace the entry throws and leaves no temporary file`() {
        fileSystem.createDirectories(dir = directory / "radar.json" / "occupied")

        assertFailsWith<IOException> {
            diskCache.write(
                key = "radar.json",
                value = "{}"
            )
        }
        assertEquals(expected = listOf("radar.json"), actual = fileNames())
    }

    @Test
    fun `a removed entry reads as null`() {
        diskCache.write(
            key = "radar.json",
            value = "{}"
        )

        diskCache.remove(key = "radar.json")

        assertNull(actual = diskCache.read(key = "radar.json"))
    }

    @Test
    fun `removing a missing entry does nothing`() {
        diskCache.remove(key = "radar.json")

        assertNull(actual = diskCache.read(key = "radar.json"))
    }
}