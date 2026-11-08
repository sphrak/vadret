package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pollenpreference.model.PollenPreferences

// Pollen turned off in the app means pollen is of no interest, so no pollen is pushed even for the allergens kept.
internal fun PollenPreferences.notifiedAllergenIds(): Set<String> =
    if (isPollenEnabled) {
        allergenPollenIds
    } else {
        emptySet()
    }