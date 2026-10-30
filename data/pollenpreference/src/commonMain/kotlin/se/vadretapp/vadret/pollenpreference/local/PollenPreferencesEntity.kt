package se.vadretapp.vadret.pollenpreference.local

internal data class PollenPreferencesEntity(
    val isPollenEnabled: Boolean?,
    val allergenPollenIds: Set<String>?,
    val isHibernationNoticeShown: Boolean?,
    val isEndOfSeasonSimulated: Boolean?
)