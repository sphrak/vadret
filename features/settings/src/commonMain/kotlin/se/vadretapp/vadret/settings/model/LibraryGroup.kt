package se.vadretapp.vadret.settings.model

import kotlinx.collections.immutable.ImmutableList
import se.vadretapp.vadret.library.model.Library

internal data class LibraryGroup(
    val author: String,
    val libraries: ImmutableList<Library>
)