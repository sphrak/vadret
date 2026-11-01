package se.vadretapp.vadret.library

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.library.local.LibraryInMemoryDataSource
import se.vadretapp.vadret.library.model.Library

internal class DefaultLibraryRepository(
    private val libraryInMemoryDataSource: LibraryInMemoryDataSource
) : LibraryRepository {

    override fun getLibraries(): Either<Nothing, List<Library>> =
        libraryInMemoryDataSource
            .getLibraries()
            .sortedBy { library: Library -> library.title }
            .asRight()
}