package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import se.vadretapp.vadret.library.LibraryRepository
import se.vadretapp.vadret.library.model.Library

internal class DefaultGetLibrariesTask(
    private val libraryRepository: LibraryRepository
) : GetLibrariesTask {

    override fun invoke(): Either<Nothing, List<Library>> = libraryRepository.getLibraries()
}