package se.vadretapp.vadret.library.local

import se.vadretapp.vadret.library.model.Library

internal interface LibraryInMemoryDataSource {
    fun getLibraries(): List<Library>
}