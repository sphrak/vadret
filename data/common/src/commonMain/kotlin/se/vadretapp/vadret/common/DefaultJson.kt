package se.vadretapp.vadret.common

import kotlinx.serialization.json.Json

val defaultJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}