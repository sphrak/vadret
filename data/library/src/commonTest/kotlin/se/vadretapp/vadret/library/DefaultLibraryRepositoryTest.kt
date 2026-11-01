package se.vadretapp.vadret.library

import io.github.sphrak.either.Either
import se.vadretapp.vadret.library.model.Library
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultLibraryRepositoryTest {

    private val libraries: List<Library> = (LibraryRepository().getLibraries() as Either.Right).b

    @Test
    fun `libraries are listed by title`() {
        assertEquals(
            expected = libraries.map { library: Library -> library.title }.sorted(),
            actual = libraries.map { library: Library -> library.title }
        )
    }

    @Test
    fun `every title is listed once`() {
        assertEquals(
            expected = libraries.size,
            actual = libraries.map { library: Library -> library.title }.toSet().size
        )
    }

    @Test
    fun `libraries the app no longer uses are not credited`() {
        val titles: Set<String> = libraries.map { library: Library -> library.title }.toSet()

        assertTrue(
            actual = titles.none { title: String ->
                title in setOf(
                    "Timber",
                    "Corbind",
                    "PermissionsDispatcher",
                    "ThreetenABP",
                    "Picasso"
                )
            }
        )
    }
}