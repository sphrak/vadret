package se.vadretapp.vadret.pushalert.local

import se.vadretapp.vadret.pushalert.model.PushAlertPlaceNamesEntity

internal interface PushAlertPlaceNameLocalDataSource {
    suspend fun save(pushAlertPlaceNamesEntity: PushAlertPlaceNamesEntity)
}