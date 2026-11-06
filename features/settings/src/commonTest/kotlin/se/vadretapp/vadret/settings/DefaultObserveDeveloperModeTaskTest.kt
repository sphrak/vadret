package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.developermode.DeveloperModeRepository
import se.vadretapp.vadret.settings.usecase.DefaultObserveDeveloperModeTask
import se.vadretapp.vadret.settings.usecase.ObserveDeveloperModeTask
import kotlin.test.Test
import kotlin.test.assertEquals

private class FailingDeveloperModeRepository(
    private val failure: Failure
) : DeveloperModeRepository {
    override fun observeIsDeveloperModeUnlocked(): Flow<Either<Failure, Boolean>> = flowOf(failure.asLeft())
    override suspend fun unlockDeveloperMode(): Either<Failure, Unit> = failure.asLeft()
}

class DefaultObserveDeveloperModeTaskTest {

    @Test
    fun `a developer build is in developer mode even when nothing was unlocked`() =
        runTest {
            val observeDeveloperModeTask: ObserveDeveloperModeTask = DefaultObserveDeveloperModeTask(
                developerModeRepository = FakeDeveloperModeRepository(isUnlocked = false),
                appInfo = developerAppInfo
            )

            assertEquals(expected = true.asRight(), actual = observeDeveloperModeTask().first())
        }

    @Test
    fun `another build is in developer mode only once it is unlocked`() =
        runTest {
            val unlocked: List<Either<Failure, Boolean>> = listOf(false, true).map { isUnlocked: Boolean ->
                DefaultObserveDeveloperModeTask(
                    developerModeRepository = FakeDeveloperModeRepository(isUnlocked = isUnlocked),
                    appInfo = releaseAppInfo
                )().first()
            }

            assertEquals(expected = listOf(false.asRight(), true.asRight()), actual = unlocked)
        }

    @Test
    fun `an unlock that cannot be read is the failure in another build`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)

            assertEquals(
                expected = failure.asLeft(),
                actual = DefaultObserveDeveloperModeTask(
                    developerModeRepository = FailingDeveloperModeRepository(failure = failure),
                    appInfo = releaseAppInfo
                )().first()
            )
        }
}