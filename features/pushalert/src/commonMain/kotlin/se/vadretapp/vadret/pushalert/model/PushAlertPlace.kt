package se.vadretapp.vadret.pushalert.model

internal sealed interface PushAlertPlace {
    data class ChosenPlace(val name: String?) : PushAlertPlace

    data class SavedPlace(
        val favoriteId: String,
        val name: String
    ) : PushAlertPlace
}