package se.vadretapp.vadret.refresh

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicBoolean

fun <L, T> Flow<Either<L, T>>.whileRefreshing(
    refresh: suspend () -> Either<L, T>,
    isNothingSaved: (T) -> Boolean,
    whenRefreshFails: (T) -> T,
    isRefreshFirst: Boolean
): Flow<Either<L, T>> =
    channelFlow {
        val refreshed: Deferred<Either<L, T>> = async(start = CoroutineStart.LAZY) { refresh() }
        val isRefreshStarted = AtomicBoolean(value = isRefreshFirst)
        val isWaitingForRefresh = AtomicBoolean(value = isRefreshFirst)
        val isRefreshDone: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
        if (isRefreshFirst) {
            refreshed.await()
            isRefreshDone.value = true
        }
        val saved: Flow<Either<L, T>> = this@whileRefreshing.onEach { savedData: Either<L, T> ->
            if (isRefreshStarted.compareAndSet(expectedValue = false, newValue = true)) {
                isWaitingForRefresh.store(newValue = savedData is Either.Right && isNothingSaved(savedData.b))
                refreshed.start()
                launch {
                    refreshed.join()
                    isRefreshDone.value = true
                }
            }
        }
        combine(
            flow = saved,
            flow2 = isRefreshDone
        ) { savedData: Either<L, T>, isDone: Boolean ->
            shown(
                saved = savedData,
                refreshed = if (isDone) {
                    refreshed.await()
                } else {
                    null
                },
                isNothingSaved = isNothingSaved,
                whenRefreshFails = whenRefreshFails,
                isWaitingForRefresh = isWaitingForRefresh.load()
            )
        }.collect { shownData: Either<L, T>? ->
            if (shownData != null) {
                send(element = shownData)
            }
        }
    }.distinctUntilChanged()

private fun <L, T> shown(
    saved: Either<L, T>,
    refreshed: Either<L, T>?,
    isNothingSaved: (T) -> Boolean,
    whenRefreshFails: (T) -> T,
    isWaitingForRefresh: Boolean
): Either<L, T>? =
    when {
        saved is Either.Left -> saved
        refreshed == null && isWaitingForRefresh -> null
        isNothingSaved((saved as Either.Right).b) -> refreshed
        refreshed is Either.Left -> whenRefreshFails(saved.b).asRight()
        else -> saved
    }