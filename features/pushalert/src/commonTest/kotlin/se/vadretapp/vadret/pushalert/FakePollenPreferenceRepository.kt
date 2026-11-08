package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences

internal class FakePollenPreferenceRepository(
    pollenPreferences: Either<Failure, PollenPreferences> = NO_ALLERGENS.asRight()
) : PollenPreferenceRepository {

    val pollenPreferences: MutableStateFlow<Either<Failure, PollenPreferences>> = MutableStateFlow(value = pollenPreferences)

    override suspend fun getPollenPreferences(): Either<Failure, PollenPreferences> = pollenPreferences.value

    override fun observePollenPreferences(): Flow<Either<Failure, PollenPreferences>> = pollenPreferences

    override suspend fun setPollenEnabled(isEnabled: Boolean): Either<Failure, Unit> = error("Push alerts never turn pollen on or off")

    override suspend fun setAllergen(
        pollenId: String,
        isAllergen: Boolean
    ): Either<Failure, Unit> = error("Push alerts never pick an allergen")

    override suspend fun setHibernationNoticeShown(isShown: Boolean): Either<Failure, Unit> = error("Push alerts never show the hibernation notice")

    override suspend fun setEndOfSeasonSimulated(isSimulated: Boolean): Either<Failure, Unit> = error("Push alerts never simulate the end of the season")
}

internal val NO_ALLERGENS: PollenPreferences = PollenPreferences(
    isPollenEnabled = true,
    allergenPollenIds = emptySet(),
    isHibernationNoticeShown = true,
    isEndOfSeasonSimulated = false
)