package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.favorite.model.Favorite

internal val STOCKHOLM_NORRMALM_FAVORITE = favorite(
    locality = "Norrmalm",
    latitude = 59.3340,
    longitude = 18.0630
)
internal val SOLNA_FAVORITE = favorite(
    locality = "Solna",
    latitude = 59.3600,
    longitude = 18.0000
)
internal val MALMO_FAVORITE = favorite(
    locality = "Malmö",
    latitude = 55.6050,
    longitude = 13.0038
)
internal val MALMO_CENTRAL_FAVORITE = favorite(
    locality = "Malmö C",
    latitude = 55.6090,
    longitude = 13.0000
)
internal val LIMHAMN_FAVORITE = favorite(
    locality = "Limhamn",
    latitude = 55.5800,
    longitude = 12.9300
)
internal val GOTEBORG_FAVORITE = favorite(
    locality = "Göteborg",
    latitude = 57.7089,
    longitude = 11.9746
)
internal val UPPSALA_FAVORITE = favorite(
    locality = "Uppsala",
    latitude = 59.8586,
    longitude = 17.6389
)
internal val UMEA_FAVORITE = favorite(
    locality = "Umeå",
    latitude = 63.8258,
    longitude = 20.2630
)
internal val KIRUNA_FAVORITE = favorite(
    locality = "Kiruna",
    latitude = 67.8558,
    longitude = 20.2253
)
internal val VISBY_FAVORITE = favorite(
    locality = "Visby",
    latitude = 57.6348,
    longitude = 18.2948
)
internal val BERLIN_FAVORITE = favorite(
    locality = "Berlin",
    latitude = 52.52,
    longitude = 13.405
)

private fun favorite(
    locality: String,
    latitude: Double,
    longitude: Double
): Favorite = Favorite(
    favoriteId = "favorite-$locality",
    locality = locality,
    municipality = locality,
    county = locality,
    latitude = latitude,
    longitude = longitude
)