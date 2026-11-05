package se.vadretapp.vadret.ui

import androidx.compose.runtime.Composable

@Composable
expect fun LocationPermissionEffect(
    isRequested: Boolean,
    onResult: (isGranted: Boolean) -> Unit
)