package se.vadretapp.vadret.followedplace

// The server follows at most five places for an installation, and the chosen place is always one of them.
internal const val MAXIMUM_FOLLOWED_PLACES = 5
internal const val MAXIMUM_FOLLOWED_FAVORITES = MAXIMUM_FOLLOWED_PLACES - 1