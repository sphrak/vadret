package se.vadretapp.vadret.common.extension

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import se.vadretapp.vadret.common.Failure

fun <T> Flow<Either<Failure, T>>.whileRefreshing(
    refresh: suspend () -> Either<Failure, T>,
    isNothingSaved: (T) -> Boolean,
    whenRefreshFails: (T) -> T,
    isRefreshFirst: Boolean
): Flow<Either<Failure, T>> =
    channelFlow {
        val refreshed: Deferred<Either<Failure, T>> = async { refresh() }
        val isRefreshDone: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
        launch {
            refreshed.join()
            isRefreshDone.value = true
        }
        if (isRefreshFirst) {
            refreshed.join()
        }
        combine(
            flow = this@whileRefreshing,
            flow2 = isRefreshDone
        ) { saved: Either<Failure, T>, isDone: Boolean ->
            shown(
                saved = saved,
                refreshed = if (isDone) {
                    refreshed.await()
                } else {
                    null
                },
                isNothingSaved = isNothingSaved,
                whenRefreshFails = whenRefreshFails,
                isRefreshFirst = isRefreshFirst
            )
        }.collect { shownData: Either<Failure, T>? ->
            if (shownData != null) {
                send(element = shownData)
            }
        }
    }.distinctUntilChanged()

private fun <T> shown(
    saved: Either<Failure, T>,
    refreshed: Either<Failure, T>?,
    isNothingSaved: (T) -> Boolean,
    whenRefreshFails: (T) -> T,
    isRefreshFirst: Boolean
): Either<Failure, T>? =
    when {
        saved is Either.Left -> saved
        isNothingSaved((saved as Either.Right).b) -> refreshed
        refreshed == null && isRefreshFirst -> null
        refreshed is Either.Left -> whenRefreshFails(saved.b).asRight()
        else -> saved
    }