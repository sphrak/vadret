package fi.kroon.vadret.pushalert.extension

import fi.kroon.vadret.pushalert.model.ChallengeDto

internal fun ChallengeDto.toChallenge(): String = checkNotNull(value = challenge) { "The server sent a challenge without a challenge" }