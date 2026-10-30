package fi.kroon.vadret.radar

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.location.model.Location
import fi.kroon.vadret.radar.extension.toRadarFrames
import fi.kroon.vadret.radar.extension.toWindTimeFrames
import fi.kroon.vadret.radar.extension.withoutMotion
import fi.kroon.vadret.radar.model.Radar
import fi.kroon.vadret.radar.model.RadarFrame
import fi.kroon.vadret.radar.model.RadarLayer
import fi.kroon.vadret.radar.model.WindTimeFrame
import fi.kroon.vadret.radar.precipitation.PrecipitationOverlay
import fi.kroon.vadret.radar.precipitation.PrecipitationRenderer
import fi.kroon.vadret.radar.precipitation.RadarEchoes
import fi.kroon.vadret.radar.wind.WindField
import fi.kroon.vadret.radar.wind.WindFieldRenderer
import fi.kroon.vadret.radar.wind.WindPalette
import fi.kroon.vadret.ui.extension.launchReplacing
import fi.kroon.vadret.ui.extension.messageResource
import fi.kroon.vadret.ui.format.currentLanguageTag
import fi.kroon.vadret.wind.model.WindForecast
import fi.kroon.vadret.wind.model.WindFrame
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.onResult
import io.github.sphrak.either.extension.successOrNull
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val PRELOADED_FRAME_DISTANCE = 3

private val frameDuration: Duration = 128.milliseconds
private const val WIND_STEPS_PER_FRAME = 4

private class PreparedWindField(
    val index: Int,
    val windField: Deferred<WindField>
)

@Immutable
class RadarViewModel internal constructor(
    private val getRadarTask: GetRadarTask,
    private val getWindForecastTask: GetWindForecastTask,
    private val getDeviceLocationTask: GetDeviceLocationTask,
    private val radarImageLoader: RadarImageLoader,
    private val precipitationRenderer: PrecipitationRenderer,
    private val windFieldRenderer: WindFieldRenderer,
    private val clock: Clock,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<RadarState>
        field = MutableStateFlow(value = RadarState())

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)
    private val isWindRequested: AtomicBoolean = AtomicBoolean(value = false)
    private val languageTag: AtomicReference<String> = AtomicReference(value = currentLanguageTag())
    private val windPalette: AtomicReference<WindPalette> = AtomicReference(value = WindPalette.DAY)
    private val radar: AtomicReference<Radar?> = AtomicReference(value = null)
    private val windForecast: AtomicReference<WindForecast?> = AtomicReference(value = null)
    private val playJob: AtomicReference<Job?> = AtomicReference(value = null)
    private val frameJob: AtomicReference<Job?> = AtomicReference(value = null)
    private val preparedWindField: AtomicReference<PreparedWindField?> = AtomicReference(value = null)

    internal fun onEvent(event: RadarEvent) {
        when (event) {
            is RadarEvent.OnStart -> onStart(initialImageUrl = event.initialImageUrl)
            RadarEvent.OnLeft -> stopPlaying()
            is RadarEvent.OnLocaleChanged -> onLocaleChanged(languageTag = event.languageTag)
            is RadarEvent.OnDarkThemeChanged -> onDarkThemeChanged(isDarkTheme = event.isDarkTheme)
            RadarEvent.OnMapLoaded -> state.update { state: RadarState ->
                state.copy(isMapLoaded = true)
            }
            RadarEvent.OnPlayButtonClicked -> onPlayButtonClicked()
            RadarEvent.OnErrorShown -> state.update { state: RadarState ->
                state.copy(errorMessageResource = null)
            }
            is RadarEvent.OnIndexChanged -> onIndexChanged(index = event.index)
            is RadarEvent.OnLayerSelected -> onLayerSelected(layer = event.layer)
            RadarEvent.OnRadarStationsToggled -> state.update { state: RadarState ->
                state.copy(areRadarStationsShown = !state.areRadarStationsShown)
            }
        }
    }

    private fun onStart(initialImageUrl: String?) {
        if (isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            loadPrecipitation(initialImageUrl = initialImageUrl)
            loadDeviceLocation()
        }
    }

    private fun loadPrecipitation(initialImageUrl: String?) {
        viewModelScope.launch(context = coroutineDispatcher) {
            getRadarTask(imageUrl = initialImageUrl)
                .onResult(
                    onError = { failure: Failure -> displayPrecipitationError(failure = failure) },
                    onSuccess = { loadedRadar: Radar ->
                        radar.store(newValue = loadedRadar)
                        val updatedState: RadarState = state.updateAndGet { state: RadarState ->
                            val frames: ImmutableList<RadarFrame> = loadedRadar.toFrames()
                            state.copy(
                                precipitationFrames = frames,
                                precipitationIndex = frames.initialIndex(initialImageUrl = initialImageUrl),
                                isPrecipitationUnavailable = frames.isEmpty(),
                                isLoading = state.isLoading && state.layer == RadarLayer.WIND && state.windField == null
                            )
                        }
                        if (updatedState.layer == RadarLayer.PRECIPITATION) {
                            showFrame(
                                layer = RadarLayer.PRECIPITATION,
                                index = updatedState.precipitationIndex
                            )
                        }
                    }
                )
        }
    }

    private fun onLocaleChanged(languageTag: String) {
        if (this.languageTag.exchange(newValue = languageTag) == languageTag) {
            return
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            state.update { state: RadarState ->
                state.copy(
                    precipitationFrames = radar.load()?.toFrames() ?: state.precipitationFrames,
                    windFrames = windForecast.load()?.toTimeFrames() ?: state.windFrames
                )
            }
        }
    }

    private fun onDarkThemeChanged(isDarkTheme: Boolean) {
        val palette: WindPalette = if (isDarkTheme) {
            WindPalette.NIGHT
        } else {
            WindPalette.DAY
        }
        if (windPalette.exchange(newValue = palette) == palette || state.value.windField == null) {
            return
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            loadWindFrame(index = state.value.windIndex)
        }
    }

    private fun ImmutableList<RadarFrame>.initialIndex(initialImageUrl: String?): Int {
        val initialImageIndex: Int = indexOfFirst { frame: RadarFrame -> frame.imageUrl == initialImageUrl }
        return if (initialImageIndex >= 0) {
            initialImageIndex
        } else {
            lastIndex.coerceAtLeast(minimumValue = 0)
        }
    }

    private fun Radar.toFrames(): ImmutableList<RadarFrame> =
        toRadarFrames(
            now = clock.now(),
            timeZone = TimeZone.currentSystemDefault(),
            languageTag = languageTag.load()
        ).toImmutableList()

    private fun WindForecast.toTimeFrames(): ImmutableList<WindTimeFrame> =
        toWindTimeFrames(
            timeZone = TimeZone.currentSystemDefault(),
            languageTag = languageTag.load()
        ).toImmutableList()

    private fun loadDeviceLocation() {
        viewModelScope.launch(context = coroutineDispatcher) {
            val location: Location? = getDeviceLocationTask().successOrNull
            if (location != null) {
                state.update { state: RadarState ->
                    state.copy(deviceLocation = location)
                }
            }
        }
    }

    private fun onLayerSelected(layer: RadarLayer) {
        if (layer == state.value.layer) {
            return
        }
        stopPlaying()
        frameJob.exchange(newValue = null)?.cancel()
        val updatedState: RadarState = state.updateAndGet { state: RadarState ->
            state.copy(layer = layer)
        }
        if (layer == RadarLayer.WIND && isWindRequested.compareAndSet(expectedValue = false, newValue = true)) {
            loadWind()
        }
        if (updatedState.isFrameMissing(layer = layer)) {
            showFrame(
                layer = layer,
                index = updatedState.index(layer = layer)
            )
        }
    }

    private fun loadWind() {
        state.update { state: RadarState ->
            state.copy(isLoading = true)
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            getWindForecastTask()
                .onResult(
                    onError = { failure: Failure ->
                        isWindRequested.store(newValue = false)
                        displayError(failure = failure)
                    },
                    onSuccess = { forecast: WindForecast ->
                        windForecast.store(newValue = forecast)
                        val updatedState: RadarState = state.updateAndGet { state: RadarState ->
                            state.copy(
                                windFrames = forecast.toTimeFrames(),
                                windIndex = 0,
                                isLoading = state.layer == RadarLayer.WIND
                            )
                        }
                        if (updatedState.layer == RadarLayer.WIND) {
                            showFrame(
                                layer = RadarLayer.WIND,
                                index = 0
                            )
                        }
                    }
                )
        }
    }

    private fun onIndexChanged(index: Int) {
        stopPlaying()
        val layer: RadarLayer = state.value.layer
        state.update { state: RadarState ->
            when (layer) {
                RadarLayer.PRECIPITATION -> state.copy(precipitationIndex = index)
                RadarLayer.WIND -> state.copy(windIndex = index)
            }
        }
        showFrame(
            layer = layer,
            index = index
        )
    }

    private fun showFrame(
        layer: RadarLayer,
        index: Int
    ) {
        viewModelScope.launchReplacing(
            currentJob = frameJob,
            context = coroutineDispatcher
        ) {
            loadFrame(
                layer = layer,
                index = index
            )
        }
    }

    private suspend fun loadFrame(
        layer: RadarLayer,
        index: Int
    ): Boolean =
        when (layer) {
            RadarLayer.PRECIPITATION -> showPrecipitationFrame(
                index = index,
                precipitationOverlay = precipitationOverlay(
                    index = index,
                    isMoving = false
                )
            )
            RadarLayer.WIND -> loadWindFrame(index = index)
        }

    private suspend fun precipitationOverlay(
        index: Int,
        isMoving: Boolean
    ): Either<Failure, PrecipitationOverlay>? {
        val frames: ImmutableList<RadarFrame> = state.value.precipitationFrames
        val frame: RadarFrame = frames.getOrNull(index = index) ?: return null
        return radarImageLoader
            .load(imageUrl = frame.imageUrl)
            .onResult(
                onError = { failure: Failure -> failure.asLeft() },
                onSuccess = { echoes: RadarEchoes ->
                    val nextFrame: RadarFrame? = if (isMoving) {
                        frames.getOrNull(index = index + 1)
                    } else {
                        null
                    }
                    renderPrecipitation(
                        echoes = echoes,
                        nextFrame = nextFrame
                    ).asRight()
                }
            )
    }

    private fun showPrecipitationFrame(
        index: Int,
        precipitationOverlay: Either<Failure, PrecipitationOverlay>?
    ): Boolean {
        if (precipitationOverlay == null) {
            return false
        }
        return precipitationOverlay.onResult(
            onError = { failure: Failure ->
                displayPrecipitationError(failure = failure)
                false
            },
            onSuccess = { shownOverlay: PrecipitationOverlay ->
                state.update { state: RadarState ->
                    state.copy(
                        precipitationIndex = index,
                        precipitationOverlay = shownOverlay
                    )
                }
                true
            }
        )
    }

    private suspend fun renderPrecipitation(
        echoes: RadarEchoes,
        nextFrame: RadarFrame?
    ): PrecipitationOverlay {
        if (nextFrame == null) {
            return precipitationRenderer.render(echoes = echoes)
        }
        val nextEchoes: RadarEchoes = radarImageLoader.load(imageUrl = nextFrame.imageUrl).successOrNull
            ?: return precipitationRenderer.render(echoes = echoes)
        return precipitationRenderer.renderMotion(
            echoes = echoes,
            nextEchoes = nextEchoes,
            duration = frameDuration
        )
    }

    private suspend fun loadWindFrame(index: Int): Boolean {
        val forecast: WindForecast = windForecast.load() ?: return false
        val windFrame: WindFrame = forecast.frames.getOrNull(index = index) ?: return false
        showWindField(
            index = index,
            windField = renderWindField(
                forecast = forecast,
                windFrame = windFrame
            )
        )
        return true
    }

    private suspend fun renderWindField(
        forecast: WindForecast,
        windFrame: WindFrame
    ): WindField =
        windFieldRenderer.render(
            windGrid = forecast.grid,
            windFrame = windFrame,
            windPalette = windPalette.load()
        )

    private fun showWindField(
        index: Int,
        windField: WindField
    ) {
        state.update { state: RadarState ->
            state.copy(
                windIndex = index,
                windField = windField,
                windTransitionField = null,
                isLoading = false
            )
        }
    }

    private fun onPlayButtonClicked() {
        if (state.value.isPlaying) {
            stopPlaying()
            return
        }
        state.update { state: RadarState ->
            state.copy(isPlaying = true)
        }
        frameJob.exchange(newValue = null)?.cancel()
        viewModelScope.launchReplacing(
            currentJob = playJob,
            context = coroutineDispatcher
        ) {
            play(layer = state.value.layer)
        }
    }

    private suspend fun CoroutineScope.play(layer: RadarLayer) {
        while (isActive) {
            val radarState: RadarState = state.value
            val frameCount: Int = radarState.frameCount(layer = layer)
            if (frameCount == 0) {
                stopPlaying()
                return
            }
            val index: Int = (radarState.index(layer = layer) + 1) % frameCount
            val isShown: Boolean = when (layer) {
                RadarLayer.PRECIPITATION -> playPrecipitationFrame(index = index)
                RadarLayer.WIND -> playWindFrame(index = index)
            }
            if (!isShown) {
                stopPlaying()
                return
            }
        }
    }

    private suspend fun CoroutineScope.playPrecipitationFrame(index: Int): Boolean {
        preloadPrecipitationFrame(index = index + PRELOADED_FRAME_DISTANCE)
        val upcomingOverlay: Deferred<Either<Failure, PrecipitationOverlay>?> = async {
            precipitationOverlay(
                index = index,
                isMoving = true
            )
        }
        delay(duration = frameDuration)
        return showPrecipitationFrame(
            index = index,
            precipitationOverlay = upcomingOverlay.await()
        )
    }

    private suspend fun CoroutineScope.playWindFrame(index: Int): Boolean {
        val forecast: WindForecast = windForecast.load() ?: return false
        val radarState: RadarState = state.value
        val nextFrame: WindFrame = forecast.frames.getOrNull(index = index) ?: return false
        val upcomingField: Deferred<WindField> = takePreparedWindField(index = index) ?: async {
            renderWindField(
                forecast = forecast,
                windFrame = nextFrame
            )
        }
        prepareWindField(
            forecast = forecast,
            index = (index + 1) % forecast.frames.size
        )
        val shownField: WindField? = radarState.windField
        if (shownField == null || index < radarState.windIndex) {
            delay(duration = frameDuration)
            showWindField(
                index = index,
                windField = upcomingField.await()
            )
            return true
        }
        val nextField: WindField = upcomingField.await()
        val stepDuration: Duration = frameDuration / WIND_STEPS_PER_FRAME
        var upcomingBlend: Deferred<WindField> = blendAsync(
            windField = shownField,
            nextWindField = nextField,
            step = 1
        )
        for (step in 1 until WIND_STEPS_PER_FRAME) {
            delay(duration = stepDuration)
            val windTransitionField: WindField = upcomingBlend.await()
            state.update { state: RadarState ->
                state.copy(windTransitionField = windTransitionField)
            }
            upcomingBlend = blendAsync(
                windField = shownField,
                nextWindField = nextField,
                step = step + 1
            )
        }
        upcomingBlend.cancel()
        delay(duration = stepDuration)
        showWindField(
            index = index,
            windField = nextField
        )
        return true
    }

    private fun CoroutineScope.blendAsync(
        windField: WindField,
        nextWindField: WindField,
        step: Int
    ): Deferred<WindField> =
        async {
            windFieldRenderer.blend(
                windField = windField,
                nextWindField = nextWindField,
                progress = step.toFloat() / WIND_STEPS_PER_FRAME
            )
        }

    private fun CoroutineScope.prepareWindField(
        forecast: WindForecast,
        index: Int
    ) {
        val windFrame: WindFrame = forecast.frames.getOrNull(index = index) ?: return
        val preparedField = PreparedWindField(
            index = index,
            windField = async {
                renderWindField(
                    forecast = forecast,
                    windFrame = windFrame
                )
            }
        )
        preparedWindField.exchange(newValue = preparedField)?.windField?.cancel()
    }

    private fun takePreparedWindField(index: Int): Deferred<WindField>? {
        val preparedField: PreparedWindField = preparedWindField.exchange(newValue = null) ?: return null
        if (preparedField.index == index) {
            return preparedField.windField
        }
        preparedField.windField.cancel()
        return null
    }

    private fun CoroutineScope.preloadPrecipitationFrame(index: Int) {
        val frame: RadarFrame = state.value.precipitationFrames.getOrNull(index = index) ?: return
        launch {
            radarImageLoader.load(imageUrl = frame.imageUrl)
        }
    }

    private fun stopPlaying() {
        playJob.exchange(newValue = null)?.cancel()
        preparedWindField.exchange(newValue = null)?.windField?.cancel()
        state.update { state: RadarState ->
            state.copy(
                isPlaying = false,
                precipitationOverlay = state.precipitationOverlay?.withoutMotion(),
                windTransitionField = null
            )
        }
    }

    private fun displayPrecipitationError(failure: Failure) {
        state.update { state: RadarState ->
            state.copy(isPrecipitationUnavailable = true)
        }
        displayError(failure = failure)
    }

    private fun displayError(failure: Failure) {
        state.update { state: RadarState ->
            state.copy(
                isLoading = false,
                errorMessageResource = failure.messageResource()
            )
        }
    }

    private fun RadarState.frameCount(layer: RadarLayer): Int =
        when (layer) {
            RadarLayer.PRECIPITATION -> precipitationFrames.size
            RadarLayer.WIND -> windFrames.size
        }

    private fun RadarState.index(layer: RadarLayer): Int =
        when (layer) {
            RadarLayer.PRECIPITATION -> precipitationIndex
            RadarLayer.WIND -> windIndex
        }

    private fun RadarState.isFrameMissing(layer: RadarLayer): Boolean =
        frameCount(layer = layer) > 0 &&
            when (layer) {
                RadarLayer.PRECIPITATION -> precipitationOverlay == null
                RadarLayer.WIND -> windField == null
            }
}