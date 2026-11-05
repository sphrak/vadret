package se.vadretapp.vadret.settings

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.settings.model.LibraryGroup

internal data class LicensesState(
    val libraryGroups: ImmutableList<LibraryGroup> = persistentListOf(),
    val selectedLibrary: Library? = null
)