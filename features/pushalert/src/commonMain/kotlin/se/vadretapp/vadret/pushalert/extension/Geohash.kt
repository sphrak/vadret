package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.geohash.model.Geohash

// The server names the cell a wind or temperature alert is about by this many geohash characters.
internal fun Geohash.toCell(): String = value.take(n = CELL_LENGTH)

internal fun Geohash.isIn(cell: String): Boolean = value.startsWith(prefix = cell)

private const val CELL_LENGTH = 4