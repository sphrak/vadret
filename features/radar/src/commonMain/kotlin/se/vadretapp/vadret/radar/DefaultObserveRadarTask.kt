package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.whileRefreshing
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.model.SavedRadar

internal class DefaultObserveRadarTask(
    private val radarRepository: RadarRepository
) : ObserveRadarTask {

    override fun invoke(imageUrl: String?): Flow<Either<Failure, Radar>> =
        if (imageUrl == null) {
            radarOfToday()
        } else {
            flow { emit(value = radarRepository.getRadarContaining(imageUrl = imageUrl)) }
        }

    private fun radarOfToday(): Flow<Either<Failure, Radar>> =
        radarRepository
            .observeRadar()
            .whileRefreshing(
                refresh = { radarRepository.refreshRadar() },
                isNothingSaved = { savedRadar: SavedRadar -> savedRadar.fetchedAt == null },
                whenRefreshFails = { savedRadar: SavedRadar -> savedRadar },
                isRefreshFirst = false
            ).map { savedRadar: Either<Failure, SavedRadar> -> savedRadar.map { saved: SavedRadar -> saved.radar } }
}