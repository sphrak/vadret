package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.wind.WindRepository
import se.vadretapp.vadret.wind.model.WindForecast

internal class DefaultGetWindForecastTask(
    private val windRepository: WindRepository
) : GetWindForecastTask {

    override suspend fun invoke(): Either<Failure, WindForecast> = windRepository.getWindForecast()
}