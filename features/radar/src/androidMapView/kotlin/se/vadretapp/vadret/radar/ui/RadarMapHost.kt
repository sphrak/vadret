package se.vadretapp.vadret.radar.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import se.vadretapp.vadret.radar.ui.map.RadarCamera
import se.vadretapp.vadret.radar.ui.map.RadarMapStore
import se.vadretapp.vadret.radar.ui.map.initialRadarCamera

internal val LocalRadarMapStore: ProvidableCompositionLocal<RadarMapStore> = staticCompositionLocalOf {
    error("RadarRoute must be placed inside RadarMapHost")
}

@Composable
fun RadarMapHost(content: @Composable () -> Unit) {
    val context: Context = LocalContext.current
    val radarMapStore: RadarMapStore = rememberSaveable(saver = radarMapStoreSaver(context = context)) {
        RadarMapStore(
            context = context,
            camera = initialRadarCamera
        )
    }
    DisposableEffect(radarMapStore) {
        onDispose { radarMapStore.destroy() }
    }
    HostLifecycleEffect(radarMapStore = radarMapStore)
    CompositionLocalProvider(
        value = LocalRadarMapStore provides radarMapStore,
        content = content
    )
}

@Composable
private fun HostLifecycleEffect(radarMapStore: RadarMapStore) {
    val lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, radarMapStore) {
        val observer = LifecycleEventObserver { _, event: Lifecycle.Event ->
            radarMapStore.onHostStateChanged(hostState = event.targetState)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

private fun radarMapStoreSaver(context: Context): Saver<RadarMapStore, Any> =
    listSaver(
        save = { radarMapStore: RadarMapStore ->
            listOf(
                radarMapStore.camera.latitude,
                radarMapStore.camera.longitude,
                radarMapStore.camera.zoom
            )
        },
        restore = { (latitude: Double, longitude: Double, zoom: Double) ->
            RadarMapStore(
                context = context,
                camera = RadarCamera(
                    latitude = latitude,
                    longitude = longitude,
                    zoom = zoom
                )
            )
        }
    )