package se.vadretapp.vadret.settings.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.settings.model.RowSummary

@Composable
internal fun RowSummary.text(): String =
    when (this) {
        is RowSummary.Resource -> stringResource(resource = resource)
        is RowSummary.Plain -> text
    }