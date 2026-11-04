package se.vadretapp.vadret.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.ui.model.ElapsedTime

@Composable
fun elapsedTimeText(elapsedTime: ElapsedTime): String {
    val text: String = stringResource(resource = elapsedTime.resource)
    val count: Long? = elapsedTime.count
    return if (count != null) {
        "$count $text"
    } else {
        text
    }
}