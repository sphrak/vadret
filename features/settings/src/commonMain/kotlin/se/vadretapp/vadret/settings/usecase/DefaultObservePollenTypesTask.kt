package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.PollenRepository
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pollen.model.PollenTypes
import se.vadretapp.vadret.refresh.whileRefreshing

internal class DefaultObservePollenTypesTask(
    private val pollenRepository: PollenRepository
) : ObservePollenTypesTask {

    override fun invoke(): Flow<Either<Failure, List<PollenType>>> =
        pollenRepository
            .observePollenTypes()
            .whileRefreshing(
                refresh = { pollenRepository.refreshPollenTypes() },
                isNothingSaved = { savedPollenTypes: PollenTypes? -> savedPollenTypes == null },
                whenRefreshFails = { savedPollenTypes: PollenTypes? -> savedPollenTypes },
                isRefreshFirst = false
            ).map { savedPollenTypes: Either<Failure, PollenTypes?> ->
                savedPollenTypes.map { saved: PollenTypes? ->
                    saved?.pollenTypes.orEmpty().filter { pollenType: PollenType -> pollenType.hasForecasts }
                }
            }
}