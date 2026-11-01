package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import se.vadretapp.vadret.library.model.Library

internal interface GetLibrariesTask {
    operator fun invoke(): Either<Nothing, List<Library>>
}