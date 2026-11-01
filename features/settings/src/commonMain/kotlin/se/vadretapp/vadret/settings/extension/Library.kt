package se.vadretapp.vadret.settings.extension

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.settings.model.LibraryGroup

internal fun List<Library>.toLibraryGroups(): ImmutableList<LibraryGroup> =
    groupBy { library: Library -> library.author }
        .map { (author: String, libraries: List<Library>) ->
            LibraryGroup(
                author = author,
                libraries = libraries.toImmutableList()
            )
        }.toImmutableList()