package se.vadretapp.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class LanguageDto {
    @SerialName(value = "en")
    ENGLISH,

    @SerialName(value = "sv")
    SWEDISH,

    @SerialName(value = "da")
    DANISH,

    @SerialName(value = "de")
    GERMAN,

    @SerialName(value = "fi")
    FINNISH,

    @SerialName(value = "fr")
    FRENCH,

    @SerialName(value = "no")
    NORWEGIAN,

    @SerialName(value = "pl")
    POLISH
}