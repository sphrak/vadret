package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.ChallengeDto

internal fun ChallengeDto.toChallenge(): String = checkNotNull(value = challenge) { "The server sent a challenge without a challenge" }