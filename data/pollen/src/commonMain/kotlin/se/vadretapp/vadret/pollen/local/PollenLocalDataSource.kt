package se.vadretapp.vadret.pollen.local

internal interface PollenLocalDataSource {

    suspend fun getPollenTypes(): List<PollenTypeEntity>?
    suspend fun setPollenTypes(pollenTypeEntities: List<PollenTypeEntity>)
}