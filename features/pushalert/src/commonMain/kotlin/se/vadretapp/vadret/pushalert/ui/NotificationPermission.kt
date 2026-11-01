package se.vadretapp.vadret.pushalert.ui

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberNotificationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit