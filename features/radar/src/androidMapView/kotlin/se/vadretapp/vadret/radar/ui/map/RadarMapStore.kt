package se.vadretapp.vadret.radar.ui.map

import android.content.Context
import androidx.lifecycle.Lifecycle

internal class RadarMapStore(
    private val context: Context,
    camera: RadarCamera
) {

    var camera: RadarCamera = camera
        private set

    private var radarMapView: RadarMapView? = null
    private var hostState: Lifecycle.State = Lifecycle.State.CREATED
    private var isShown: Boolean = false

    fun radarMapView(): RadarMapView {
        val existingView: RadarMapView? = radarMapView
        if (existingView != null) {
            return existingView
        }
        val createdView = RadarMapView(
            context = context,
            initialCamera = camera,
            onCameraIdle = { radarCamera: RadarCamera -> camera = radarCamera }
        )
        radarMapView = createdView
        return createdView
    }

    fun onShownChanged(isShown: Boolean) {
        this.isShown = isShown
        moveMapToTargetState()
    }

    fun onHostStateChanged(hostState: Lifecycle.State) {
        this.hostState = hostState
        moveMapToTargetState()
    }

    fun destroy() {
        radarMapView?.destroy()
        radarMapView = null
    }

    private fun moveMapToTargetState() {
        val targetState: Lifecycle.State = if (isShown) {
            hostState
        } else {
            Lifecycle.State.CREATED
        }
        radarMapView?.moveTo(state = targetState)
    }
}