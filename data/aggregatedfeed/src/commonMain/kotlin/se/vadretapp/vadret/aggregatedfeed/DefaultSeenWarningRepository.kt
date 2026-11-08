package se.vadretapp.vadret.aggregatedfeed

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.aggregatedfeed.extension.toFailure
import se.vadretapp.vadret.aggregatedfeed.extension.toSeenWarning
import se.vadretapp.vadret.aggregatedfeed.local.SeenWarningEntity
import se.vadretapp.vadret.aggregatedfeed.local.SeenWarningLocalDataSource
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import kotlin.time.Clock

internal class DefaultSeenWarningRepository(
    private val seenWarningLocalDataSource: SeenWarningLocalDataSource,
    private val clock: Clock
) : SeenWarningRepository {

    override fun observeSeenWarnings(): Flow<Either<Failure, List<SeenWarning>>> =
        seenWarningLocalDataSource
            .observeSeenWarnings()
            .map<List<SeenWarningEntity>, Either<Failure, List<SeenWarning>>> { seenWarningEntities: List<SeenWarningEntity> ->
                seenWarningEntities
                    .map { seenWarningEntity: SeenWarningEntity -> seenWarningEntity.toSeenWarning() }
                    .asRight()
            }.catch { throwable: Throwable ->
                if (throwable is Exception) {
                    emit(throwable.toFailure().asLeft())
                } else {
                    throw throwable
                }
            }

    override suspend fun markSeen(warningIdentifier: String): Either<Failure, Unit> =
        either {
            seenWarningLocalDataSource.markSeen(
                seenWarningEntity = SeenWarningEntity(
                    warningIdentifier = warningIdentifier,
                    seenAtEpochMillis = clock.now().toEpochMilliseconds()
                )
            )
        }.catch { exception: Exception -> exception.toFailure() }
}