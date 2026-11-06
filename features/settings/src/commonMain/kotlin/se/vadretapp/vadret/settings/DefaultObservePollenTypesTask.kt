package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.whileRefreshing
import se.vadretapp.vadret.pollen.PollenRepository
import se.vadretapp.vadret.pollen.model.PollenType

internal class DefaultObservePollenTypesTask(
    private val pollenRepository: PollenRepository
) : ObservePollenTypesTask {

    override fun invoke(): Flow<Either<Failure, List<PollenType>>> =
        pollenRepository
            .observePollenTypes()
            .whileRefreshing(
                refresh = { pollenRepository.refreshPollenTypes() },
                isNothingSaved = { pollenTypes: List<PollenType> -> pollenTypes.isEmpty() },
                whenRefreshFails = { pollenTypes: List<PollenType> -> pollenTypes },
                isRefreshFirst = false
            ).map { pollenTypes: Either<Failure, List<PollenType>> ->
                pollenTypes.map { types: List<PollenType> -> types.filter { pollenType: PollenType -> pollenType.hasForecasts } }
            }
}