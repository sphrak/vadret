package se.vadretapp.vadret.nominatim.local

import se.vadretapp.vadret.nominatim.model.Locality
import kotlin.concurrent.atomics.AtomicReference

private const val REMEMBERED_LOCALITIES = 16

private data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

// The weather, its warnings and the followed places all name the same position, so Nominatim is asked once for it.
internal class DefaultLocalityInMemoryDataSource : LocalityInMemoryDataSource {

    private val localities: AtomicReference<Map<Coordinates, Locality>> = AtomicReference(value = emptyMap())

    override fun locality(
        latitude: Double,
        longitude: Double
    ): Locality? =
        localities.load()[
            Coordinates(
                latitude = latitude,
                longitude = longitude
            )
        ]

    override fun remember(
        latitude: Double,
        longitude: Double,
        locality: Locality
    ) {
        val coordinates = Coordinates(
            latitude = latitude,
            longitude = longitude
        )
        while (true) {
            val remembered: Map<Coordinates, Locality> = localities.load()
            val updated: Map<Coordinates, Locality> = (remembered - coordinates + (coordinates to locality))
                .entries
                .toList()
                .takeLast(n = REMEMBERED_LOCALITIES)
                .associate { entry: Map.Entry<Coordinates, Locality> -> entry.key to entry.value }
            if (localities.compareAndSet(expectedValue = remembered, newValue = updated)) {
                return
            }
        }
    }
}