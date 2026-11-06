package se.vadretapp.vadret.common.extension

import io.github.sphrak.either.Either
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import se.vadretapp.vadret.common.Failure

fun <T> Flow<Either<Failure, T>>.whileRefreshing(
    refresh: suspend () -> Either<Failure, T>,
    isNothingSaved: (T) -> Boolean
): Flow<Either<Failure, T>> =
    channelFlow {
        val refreshed: Deferred<Either<Failure, T>> = async { refresh() }
        collect { saved: Either<Failure, T> ->
            if (saved is Either.Right && isNothingSaved(saved.b)) {
                send(element = refreshed.await())
            } else {
                send(element = saved)
            }
        }
    }.distinctUntilChanged()