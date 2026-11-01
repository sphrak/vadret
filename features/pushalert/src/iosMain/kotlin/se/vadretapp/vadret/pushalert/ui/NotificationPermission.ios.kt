package se.vadretapp.vadret.pushalert.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter

@Composable
internal actual fun rememberNotificationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit {
    val currentOnResult: (Boolean) -> Unit by rememberUpdatedState(newValue = onResult)
    val coroutineScope = rememberCoroutineScope()
    return remember(coroutineScope) {
        {
            UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound
            ) { isGranted: Boolean, _ ->
                coroutineScope.launch { currentOnResult(isGranted) }
            }
        }
    }
}