package se.vadretapp.vadret.radar.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.refresh.whileRefreshing
import se.vadretapp.vadret.wind.WindRepository
import se.vadretapp.vadret.wind.model.WindForecast
import se.vadretapp.vadret.wind.model.WindFrame
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private val frameLength: Duration = 1.hours

internal class DefaultObserveWindForecastTask(
    private val windRepository: WindRepository,
    private val clock: Clock
) : ObserveWindForecastTask {

    override fun invoke(): Flow<Either<Failure, WindForecast>> =
        windRepository
            .observeWindForecast()
            .whileRefreshing(
                refresh = { windRepository.refreshWindForecast() },
                isNothingSaved = { savedWindForecast: WindForecast? -> savedWindForecast?.withoutPastHours()?.frames.isNullOrEmpty() },
                whenRefreshFails = { savedWindForecast: WindForecast? -> savedWindForecast },
                isRefreshFirst = false
            ).map { savedWindForecast: Either<Failure, WindForecast?> ->
                savedWindForecast.flatMap { saved: WindForecast? ->
                    val currentWindForecast: WindForecast? = saved?.withoutPastHours()
                    if (currentWindForecast == null || currentWindForecast.frames.isEmpty()) {
                        Failure(errorCode = ErrorCode.WIND_NOT_AVAILABLE).asLeft()
                    } else {
                        currentWindForecast.asRight()
                    }
                }
            }

    private fun WindForecast.withoutPastHours(): WindForecast {
        val now = clock.now()
        return copy(frames = frames.filter { windFrame: WindFrame -> windFrame.validAt + frameLength > now })
    }
}