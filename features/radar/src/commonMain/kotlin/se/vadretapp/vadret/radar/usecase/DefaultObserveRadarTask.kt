package se.vadretapp.vadret.radar.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.RadarRepository
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.refresh.whileRefreshing
import kotlin.time.Clock

internal class DefaultObserveRadarTask(
    private val radarRepository: RadarRepository,
    private val clock: Clock
) : ObserveRadarTask {

    override fun invoke(imageUrl: String?): Flow<Either<Failure, Radar>> =
        if (imageUrl == null) {
            radarOfToday()
        } else {
            flow { emit(value = radarRepository.getRadarContaining(imageUrl = imageUrl)) }
        }

    private fun radarOfToday(): Flow<Either<Failure, Radar>> {
        val today: LocalDate = clock.now().toLocalDateTime(timeZone = TimeZone.UTC).date
        return radarRepository
            .observeRadar(date = today)
            .whileRefreshing(
                refresh = { radarRepository.refreshRadar(date = today) },
                isNothingSaved = { savedRadar: Radar? -> savedRadar == null },
                whenRefreshFails = { savedRadar: Radar? -> savedRadar },
                isRefreshFirst = false
            ).mapNotNull { savedRadar: Either<Failure, Radar?> ->
                when (savedRadar) {
                    is Either.Left -> savedRadar
                    is Either.Right -> savedRadar.b?.asRight()
                }
            }
    }
}