package se.vadretapp.vadret.settings

import se.vadretapp.vadret.library.model.Library

internal sealed interface LicensesEvent {
    data object OnStart : LicensesEvent
    data class OnLibraryClicked(val library: Library) : LicensesEvent
    data object OnLibraryDismissed : LicensesEvent
}