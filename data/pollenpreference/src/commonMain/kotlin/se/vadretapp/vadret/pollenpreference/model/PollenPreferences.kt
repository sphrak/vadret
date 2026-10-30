package se.vadretapp.vadret.pollenpreference.model

data class PollenPreferences(
    val isPollenEnabled: Boolean,
    val allergenPollenIds: Set<String>,
    val isHibernationNoticeShown: Boolean,
    val isEndOfSeasonSimulated: Boolean
)