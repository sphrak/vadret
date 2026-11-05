package se.vadretapp.vadret.pollenseason

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences
import kotlin.test.Test
import kotlin.test.assertEquals

private val networkFailure = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE)

private val realSeasonPollenPreferences = PollenPreferences(
    isPollenEnabled = true,
    allergenPollenIds = emptySet(),
    isHibernationNoticeShown = false,
    isEndOfSeasonSimulated = false
)

private class FakeObservePollenInSeasonTask : ObservePollenInSeasonTask {

    val isInSeason = MutableStateFlow<Either<Failure, Boolean>>(value = true.asRight())

    override fun invoke(): Flow<Either<Failure, Boolean>> = isInSeason
}

private class ObservedPollenPreferenceRepository : PollenPreferenceRepository {

    val pollenPreferences = MutableStateFlow<Either<Failure, PollenPreferences>>(value = realSeasonPollenPreferences.asRight())

    override suspend fun getPollenPreferences(): Either<Failure, PollenPreferences> = error("The simulation only observes the preferences")

    override fun observePollenPreferences(): Flow<Either<Failure, PollenPreferences>> = pollenPreferences

    override suspend fun setPollenEnabled(isEnabled: Boolean): Either<Failure, Unit> = error("The simulation never stores preferences")

    override suspend fun setAllergen(
        pollenId: String,
        isAllergen: Boolean
    ): Either<Failure, Unit> = error("The simulation never stores preferences")

    override suspend fun setHibernationNoticeShown(isShown: Boolean): Either<Failure, Unit> = error("The simulation never stores preferences")

    override suspend fun setEndOfSeasonSimulated(isSimulated: Boolean): Either<Failure, Unit> = error("The simulation never stores preferences")
}

class EndOfSeasonSimulatingObservePollenInSeasonTaskTest {

    private val observePollenInSeasonTask = FakeObservePollenInSeasonTask()
    private val pollenPreferenceRepository = ObservedPollenPreferenceRepository()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a simulated end of season overrides the real season until it is turned off`() =
        runTest {
            val emitted = mutableListOf<Either<Failure, Boolean>>()
            val job = launch {
                EndOfSeasonSimulatingObservePollenInSeasonTask(
                    observePollenInSeasonTask = observePollenInSeasonTask,
                    pollenPreferenceRepository = pollenPreferenceRepository
                )().toList(destination = emitted)
            }
            advanceUntilIdle()

            pollenPreferenceRepository.pollenPreferences.value = realSeasonPollenPreferences.copy(isEndOfSeasonSimulated = true).asRight()
            advanceUntilIdle()
            observePollenInSeasonTask.isInSeason.value = networkFailure.asLeft()
            advanceUntilIdle()
            pollenPreferenceRepository.pollenPreferences.value = realSeasonPollenPreferences.copy(
                isEndOfSeasonSimulated = true,
                isHibernationNoticeShown = true
            ).asRight()
            advanceUntilIdle()
            pollenPreferenceRepository.pollenPreferences.value = realSeasonPollenPreferences.asRight()
            advanceUntilIdle()
            observePollenInSeasonTask.isInSeason.value = true.asRight()
            advanceUntilIdle()
            job.cancel()

            assertEquals(
                expected = listOf(
                    true.asRight(),
                    false.asRight(),
                    networkFailure.asLeft(),
                    true.asRight()
                ),
                actual = emitted
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `preferences that cannot be read follow the real season`() =
        runTest {
            pollenPreferenceRepository.pollenPreferences.value = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft()
            observePollenInSeasonTask.isInSeason.value = false.asRight()
            val emitted = mutableListOf<Either<Failure, Boolean>>()
            val job = launch {
                EndOfSeasonSimulatingObservePollenInSeasonTask(
                    observePollenInSeasonTask = observePollenInSeasonTask,
                    pollenPreferenceRepository = pollenPreferenceRepository
                )().toList(destination = emitted)
            }
            advanceUntilIdle()
            job.cancel()

            assertEquals(
                expected = listOf<Either<Failure, Boolean>>(false.asRight()),
                actual = emitted
            )
        }
}