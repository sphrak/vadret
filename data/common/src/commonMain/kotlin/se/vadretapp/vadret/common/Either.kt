package se.vadretapp.vadret.common

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlin.coroutines.cancellation.CancellationException

inline fun <R> either(block: () -> R): Either<Exception, R> =
    try {
        block().asRight()
    } catch (cancellationException: CancellationException) {
        throw cancellationException
    } catch (exception: Exception) {
        exception.asLeft()
    }

inline fun <L, R> Either<Exception, R>.catch(transform: (Exception) -> L): Either<L, R> =
    when (this) {
        is Either.Left -> transform(a).asLeft()
        is Either.Right -> b.asRight()
    }