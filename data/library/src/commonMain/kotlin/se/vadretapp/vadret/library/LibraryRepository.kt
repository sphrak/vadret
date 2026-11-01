package se.vadretapp.vadret.library

import io.github.sphrak.either.Either
import se.vadretapp.vadret.library.local.DefaultLibraryInMemoryDataSource
import se.vadretapp.vadret.library.model.Library

interface LibraryRepository {

    fun getLibraries(): Either<Nothing, List<Library>>
}

fun LibraryRepository(): LibraryRepository = DefaultLibraryRepository(libraryInMemoryDataSource = DefaultLibraryInMemoryDataSource())