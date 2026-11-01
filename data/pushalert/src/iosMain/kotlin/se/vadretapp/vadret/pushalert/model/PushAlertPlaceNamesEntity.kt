package se.vadretapp.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PushAlertPlaceNamesEntity(
    @SerialName(value = "place_names_by_cell")
    val placeNamesByCell: Map<String, String>
)