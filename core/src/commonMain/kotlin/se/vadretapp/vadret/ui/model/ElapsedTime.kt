package se.vadretapp.vadret.ui.model

import org.jetbrains.compose.resources.StringResource

data class ElapsedTime(
    val resource: StringResource,
    val count: Long? = null
)