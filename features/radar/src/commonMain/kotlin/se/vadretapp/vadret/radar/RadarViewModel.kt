package se.vadretapp.vadret.radar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.onResult
import io.github.sphrak.either.extension.successOrNull
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences
import se.vadretapp.vadret.pollenseason.ObservePollenInSeasonTask
import se.vadretapp.vadret.radar.extension.markersFor
import se.vadretapp.vadret.radar.extension.pollenTypesWithAllergensFirst
import se.vadretapp.vadret.radar.extension.strongestPollenId
import se.vadretapp.vadret.radar.extension.toDateText
import se.vadretapp.vadret.radar.extension.toDayText
import se.vadretapp.vadret.radar.extension.toRadarFrames
import se.vadretapp.vadret.radar.extension.toWindTimeFrames
import se.vadretapp.vadret.radar.extension.withoutMotion
import se.vadretapp.vadret.radar.model.PollenMap
import se.vadretapp.vadret.radar.model.PollenMarker
import se.vadretapp.vadret.radar.model.PollenSeasonStatus
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.model.RadarFrame
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.model.WindTimeFrame
import se.vadretapp.vadret.radar.pollen.PollenCloudRenderer
import se.vadretapp.vadret.radar.precipitation.PrecipitationOverlay
import se.vadretapp.vadret.radar.precipitation.PrecipitationRenderer
import se.vadretapp.vadret.radar.precipitation.RadarEchoes
import se.vadretapp.vadret.radar.usecase.DownloadRadarDayTask
import se.vadretapp.vadret.radar.usecase.GetDeviceLocationTask
import se.vadretapp.vadret.radar.usecase.GetPollenMapTask
import se.vadretapp.vadret.radar.usecase.ObservePollenPreferencesTask
import se.vadretapp.vadret.radar.usecase.ObserveRadarTask
import se.vadretapp.vadret.radar.usecase.ObserveWindForecastTask
import se.vadretapp.vadret.radar.usecase.SetPollenHibernationNoticeShownTask
import se.vadretapp.vadret.radar.wind.WindField
import se.vadretapp.vadret.radar.wind.WindFieldRenderer
import se.vadretapp.vadret.radar.wind.WindPalette
import se.vadretapp.vadret.ui.extension.launchReplacing
import se.vadretapp.vadret.ui.extension.messageResource
import se.vadretapp.vadret.ui.format.currentLanguageTag
import se.vadretapp.vadret.wind.model.WindForecast
import se.vadretapp.vadret.wind.model.WindFrame
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val PRELOADED_FRAME_DISTANCE = 3

private val frameDuration: Duration = 128.milliseconds
private val slowLoadDelay: Duration = 300.milliseconds
private const val WIND_STEPS_PER_FRAME = 4

private class PreparedWindField(
    val index: Int,
    val windField: Deferred<WindField>
)

@Immutable
class RadarViewModel internal constructor(
    private val observeRadarTask: ObserveRadarTask,
    private val downloadRadarDayTask: DownloadRadarDayTask,
    private val observeWindForecastTask: ObserveWindForecastTask,
    private val getDeviceLocationTask: GetDeviceLocationTask,
    private val getPollenMapTask: GetPollenMapTask,
    private val observePollenPreferencesTask: ObservePollenPreferencesTask,
    private val observePollenInSeasonTask: ObservePollenInSeasonTask,
    private val setPollenHibernationNoticeShownTask: SetPollenHibernationNoticeShownTask,
    private val radarImageLoader: RadarImageLoader,
    private val precipitationRenderer: PrecipitationRenderer,
    private val windFieldRenderer: WindFieldRenderer,
    private val pollenCloudRenderer: PollenCloudRenderer,
    private val clock: Clock,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<RadarState>
        field = MutableStateFlow(value = RadarState())

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)
    private val pollenPreferences: AtomicReference<PollenPreferences?> = AtomicReference(value = null)
    private val pollenSeasonStatus: AtomicReference<PollenSeasonStatus> = AtomicReference(value = PollenSeasonStatus.UNKNOWN)
    private val isHibernationNoticeDismissed: AtomicBoolean = AtomicBoolean(value = false)
    private val isWindRequested: AtomicBoolean = AtomicBoolean(value = false)
    private val isPollenRequested: AtomicBoolean = AtomicBoolean(value = false)
    private val pollenMap: AtomicReference<PollenMap?> = AtomicReference(value = null)
    private val pollenCloudJob: AtomicReference<Job?> = AtomicReference(value = null)
    private val languageTag: AtomicReference<String> = AtomicReference(value = currentLanguageTag())
    private val windPalette: AtomicReference<WindPalette> = AtomicReference(value = WindPalette.DAY)
    private val radar: AtomicReference<Radar?> = AtomicReference(value = null)
    private val windForecast: AtomicReference<WindForecast?> = AtomicReference(value = null)
    private val playJob: AtomicReference<Job?> = AtomicReference(value = null)
    private val frameJob: AtomicReference<Job?> = AtomicReference(value = null)
    private val radarDayJob: AtomicReference<Job?> = AtomicReference(value = null)
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
            RadarEvent.OnPollenHibernationNoticeDismissed -> onPollenHibernationNoticeDismissed()
            is RadarEvent.OnPollenTypeSelected -> onPollenTypeSelected(pollenId = event.pollenId)
            is RadarEvent.OnPegmanDropped -> onPegmanDropped(location = event.location)
            RadarEvent.OnPlaceIslandDismissed -> state.update { state: RadarState ->
                state.copy(droppedLocation = null)
            }
        }
    }

    private fun onStart(initialImageUrl: String?) {
        if (isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            loadPrecipitation(initialImageUrl = initialImageUrl)
            loadDeviceLocation()
            observePollenPreferences()
            observePollenSeasonStatus()
        }
    }

    private fun observePollenSeasonStatus() {
        viewModelScope.launch(context = coroutineDispatcher) {
            observePollenInSeasonTask().collect { result: Either<Failure, Boolean> ->
                pollenSeasonStatus.store(
                    newValue = result.onResult(
                        onError = { _: Failure -> PollenSeasonStatus.UNKNOWN },
                        onSuccess = { isInSeason: Boolean ->
                            if (isInSeason) {
                                PollenSeasonStatus.IN_SEASON
                            } else {
                                PollenSeasonStatus.OFF_SEASON
                            }
                        }
                    )
                )
                showPollenAvailability()
            }
        }
    }

    private fun observePollenPreferences() {
        viewModelScope.launch(context = coroutineDispatcher) {
            observePollenPreferencesTask().collect { result: Either<Failure, PollenPreferences> ->
                result.onResult(
                    onError = { _: Failure -> },
                    onSuccess = { pollenPreferences: PollenPreferences -> onPollenPreferencesChanged(pollenPreferences = pollenPreferences) }
                )
            }
        }
    }

    private fun onPollenPreferencesChanged(pollenPreferences: PollenPreferences) {
        this.pollenPreferences.store(newValue = pollenPreferences)
        if (pollenPreferences.isHibernationNoticeShown) {
            isHibernationNoticeDismissed.store(newValue = false)
        }
        showPollenAvailability()
        val loadedPollenMap: PollenMap? = pollenMap.load()
        if (loadedPollenMap != null) {
            state.update { state: RadarState ->
                state.copy(pollenTypes = loadedPollenMap.pollenTypesWithAllergensFirst(allergenPollenIds = pollenPreferences.allergenPollenIds))
            }
        }
    }

    private fun showPollenAvailability() {
        val storedPollenPreferences: PollenPreferences? = pollenPreferences.load()
        val isPollenShown: Boolean = storedPollenPreferences?.isPollenEnabled != false && pollenSeasonStatus.load() == PollenSeasonStatus.IN_SEASON
        if (!isPollenShown && state.value.layer == RadarLayer.POLLEN) {
            onLayerSelected(layer = RadarLayer.PRECIPITATION)
        }
        if (storedPollenPreferences?.isHibernationNoticeShown == true && pollenSeasonStatus.load() == PollenSeasonStatus.IN_SEASON) {
            viewModelScope.launch(context = coroutineDispatcher) {
                setPollenHibernationNoticeShownTask(isShown = false)
            }
        }
        state.update { state: RadarState ->
            state.copy(
                layers = if (isPollenShown) {
                    layersWithPollen
                } else {
                    layersWithoutPollen
                },
                isPollenHibernationNoticeVisible = isPollenHibernationNoticeVisible(pollenPreferences = storedPollenPreferences)
            )
        }
    }

    private fun isPollenHibernationNoticeVisible(pollenPreferences: PollenPreferences?): Boolean =
        pollenPreferences != null &&
            pollenPreferences.isPollenEnabled &&
            !pollenPreferences.isHibernationNoticeShown &&
            pollenSeasonStatus.load() == PollenSeasonStatus.OFF_SEASON &&
            !isHibernationNoticeDismissed.load()

    private fun onPollenHibernationNoticeDismissed() {
        isHibernationNoticeDismissed.store(newValue = true)
        state.update { state: RadarState ->
            state.copy(isPollenHibernationNoticeVisible = false)
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            setPollenHibernationNoticeShownTask(isShown = true)
        }
    }

    private fun loadPrecipitation(initialImageUrl: String?) {
        viewModelScope.launch(context = coroutineDispatcher) {
            observeRadarTask(imageUrl = initialImageUrl).collect { result: Either<Failure, Radar> ->
                result.onResult(
                    onError = { failure: Failure -> displayPrecipitationError(failure = failure) },
                    onSuccess = { loadedRadar: Radar ->
                        showRadar(
                            loadedRadar = loadedRadar,
                            initialImageUrl = initialImageUrl
                        )
                    }
                )
            }
        }
    }

    private fun showRadar(
        loadedRadar: Radar,
        initialImageUrl: String?
    ) {
        radar.store(newValue = loadedRadar)
        val shownState: RadarState = state.value
        val updatedState: RadarState = state.updateAndGet { state: RadarState ->
            val frames: ImmutableList<RadarFrame> = loadedRadar.toFrames()
            state.copy(
                precipitationFrames = frames,
                precipitationIndex = frames.indexAfterUpdate(
                    shownFrames = state.precipitationFrames,
                    shownIndex = state.precipitationIndex,
                    isPlaying = state.isPlaying,
                    initialImageUrl = initialImageUrl
                ),
                isPrecipitationUnavailable = frames.isEmpty(),
                isLoading = state.isLoading && state.layer == RadarLayer.WIND && state.windField == null
            )
        }
        val isShownFrameChanged: Boolean = updatedState.shownImageUrl() != shownState.shownImageUrl() || updatedState.precipitationOverlay == null
        if (updatedState.layer == RadarLayer.PRECIPITATION && !updatedState.isPlaying && isShownFrameChanged) {
            showFrame(
                layer = RadarLayer.PRECIPITATION,
                index = updatedState.precipitationIndex
            )
        }
    }

    private fun RadarState.shownImageUrl(): String? = precipitationFrames.getOrNull(index = precipitationIndex)?.imageUrl

    private fun ImmutableList<RadarFrame>.indexAfterUpdate(
        shownFrames: List<RadarFrame>,
        shownIndex: Int,
        isPlaying: Boolean,
        initialImageUrl: String?
    ): Int {
        if (shownFrames.isEmpty()) {
            return initialIndex(initialImageUrl = initialImageUrl)
        }
        if (shownIndex == shownFrames.lastIndex && !isPlaying) {
            return lastIndex.coerceAtLeast(minimumValue = 0)
        }
        val shownImageUrl: String? = shownFrames.getOrNull(index = shownIndex)?.imageUrl
        val sameFrameIndex: Int = indexOfFirst { frame: RadarFrame -> frame.imageUrl == shownImageUrl }
        return if (sameFrameIndex >= 0) {
            sameFrameIndex
        } else {
            shownIndex.coerceIn(
                minimumValue = 0,
                maximumValue = lastIndex.coerceAtLeast(minimumValue = 0)
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
                    windFrames = windForecast.load()?.toTimeFrames() ?: state.windFrames,
                    pollenDayText = pollenMap.load()?.dayText() ?: state.pollenDayText,
                    pollenDateText = pollenMap.load()?.dateText() ?: state.pollenDateText
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
        if (layer == RadarLayer.POLLEN && isPollenRequested.compareAndSet(expectedValue = false, newValue = true)) {
            loadPollen()
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
            observeWindForecastTask()
                .transformWhile { result: Either<Failure, WindForecast> ->
                    emit(value = result)
                    result is Either.Right
                }.collect { result: Either<Failure, WindForecast> ->
                    result.onResult(
                        onError = { failure: Failure ->
                            isWindRequested.store(newValue = false)
                            displayError(failure = failure)
                        },
                        onSuccess = { forecast: WindForecast -> showWindForecast(forecast = forecast) }
                    )
                }
        }
    }

    private fun showWindForecast(forecast: WindForecast) {
        val isFirstForecast: Boolean = windForecast.exchange(newValue = forecast) == null
        val updatedState: RadarState = state.updateAndGet { state: RadarState ->
            val windFrames: ImmutableList<WindTimeFrame> = forecast.toTimeFrames()
            state.copy(
                windFrames = windFrames,
                windIndex = if (isFirstForecast) {
                    0
                } else {
                    state.windIndex.coerceIn(
                        minimumValue = 0,
                        maximumValue = windFrames.lastIndex.coerceAtLeast(minimumValue = 0)
                    )
                },
                isLoading = state.layer == RadarLayer.WIND
            )
        }
        if (updatedState.layer == RadarLayer.WIND && !updatedState.isPlaying) {
            showFrame(
                layer = RadarLayer.WIND,
                index = updatedState.windIndex
            )
        } else if (updatedState.isLoading && updatedState.layer == RadarLayer.WIND) {
            state.update { state: RadarState -> state.copy(isLoading = false) }
        }
    }

    private fun loadPollen() {
        state.update { state: RadarState ->
            state.copy(isLoading = true)
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            getPollenMapTask(date = clock.todayIn(timeZone = TimeZone.currentSystemDefault()))
                .onResult(
                    onError = { failure: Failure ->
                        isPollenRequested.store(newValue = false)
                        displayError(failure = failure)
                    },
                    onSuccess = { loadedPollenMap: PollenMap ->
                        pollenMap.store(newValue = loadedPollenMap)
                        val allergenPollenIds: Set<String> = pollenPreferences.load()?.allergenPollenIds.orEmpty()
                        val selectedPollenId: String? = loadedPollenMap.strongestPollenId(allergenPollenIds = allergenPollenIds)
                        val updatedState: RadarState = state.updateAndGet { state: RadarState ->
                            state.copy(
                                pollenTypes = loadedPollenMap.pollenTypesWithAllergensFirst(allergenPollenIds = allergenPollenIds),
                                selectedPollenId = selectedPollenId,
                                pollenMarkers = loadedPollenMap.markersFor(pollenId = selectedPollenId),
                                pollenDayText = loadedPollenMap.dayText(),
                                pollenDateText = loadedPollenMap.dateText(),
                                isLoading = state.isLoading && state.layer == RadarLayer.WIND && state.windField == null
                            )
                        }
                        renderPollenClouds(pollenMarkers = updatedState.pollenMarkers)
                    }
                )
        }
    }

    private fun onPollenTypeSelected(pollenId: String) {
        viewModelScope.launch(context = coroutineDispatcher) {
            val updatedState: RadarState = state.updateAndGet { state: RadarState ->
                state.copy(
                    selectedPollenId = pollenId,
                    pollenMarkers = pollenMap.load()?.markersFor(pollenId = pollenId) ?: persistentListOf()
                )
            }
            renderPollenClouds(pollenMarkers = updatedState.pollenMarkers)
        }
    }

    private fun renderPollenClouds(pollenMarkers: ImmutableList<PollenMarker>) {
        viewModelScope.launchReplacing(
            currentJob = pollenCloudJob,
            context = coroutineDispatcher
        ) {
            val pollenClouds: ImageBitmap = pollenCloudRenderer.render(pollenMarkers = pollenMarkers)
            state.update { state: RadarState ->
                state.copy(pollenClouds = pollenClouds)
            }
        }
    }

    private fun PollenMap.dayText(): String =
        toDayText(
            timeZone = TimeZone.currentSystemDefault(),
            languageTag = languageTag.load()
        )

    private fun PollenMap.dateText(): String =
        toDateText(
            timeZone = TimeZone.currentSystemDefault(),
            languageTag = languageTag.load()
        )

    private fun onIndexChanged(index: Int) {
        stopPlaying()
        val layer: RadarLayer = state.value.layer
        if (layer == RadarLayer.PRECIPITATION) {
            downloadRadarDay()
        }
        state.update { state: RadarState ->
            when (layer) {
                RadarLayer.PRECIPITATION -> state.copy(precipitationIndex = index)
                RadarLayer.WIND -> state.copy(windIndex = index)
                RadarLayer.POLLEN -> state
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

            RadarLayer.POLLEN -> false
        }

    private suspend fun precipitationOverlay(
        index: Int,
        isMoving: Boolean
    ): Either<Failure, PrecipitationOverlay>? =
        withSlowLoadProgress(
            showProgress = { state: RadarState, isShown: Boolean -> state.copy(isWaitingForFrame = isShown) }
        ) {
            renderedPrecipitationOverlay(
                index = index,
                isMoving = isMoving
            )
        }

    // Images on disk are ready within a few frames, so the loading bar only shows while SMHI is slow to answer.
    private suspend fun <T> withSlowLoadProgress(
        showProgress: (RadarState, Boolean) -> RadarState,
        load: suspend () -> T
    ): T = coroutineScope {
        val slowLoadIndicator: Job = launch {
            delay(duration = slowLoadDelay)
            state.update { state: RadarState -> showProgress(state, true) }
        }
        try {
            load()
        } finally {
            slowLoadIndicator.cancel()
            state.update { state: RadarState -> showProgress(state, false) }
        }
    }

    // Playing or moving the slider goes through most of the day, which one archive brings in a single request.
    // It runs on its own, so moving the slider while it downloads does not start it over, and a day that fails
    // leaves its frames to load one by one.
    private fun downloadRadarDay(): Job? {
        val startedDownload: Job? = radarDayJob.load()
        if (startedDownload != null || state.value.precipitationFrames.isEmpty()) {
            return startedDownload
        }
        val radarDayDownload: Job = viewModelScope.launch(
            context = coroutineDispatcher,
            start = CoroutineStart.LAZY
        ) {
            withSlowLoadProgress(
                showProgress = { state: RadarState, isShown: Boolean -> state.copy(isDownloadingRadarDay = isShown) }
            ) {
                downloadRadarDayTask(imageUrls = state.value.precipitationFrames.map { frame: RadarFrame -> frame.imageUrl })
            }
        }
        if (radarDayJob.compareAndSet(expectedValue = null, newValue = radarDayDownload)) {
            radarDayDownload.start()
            return radarDayDownload
        }
        radarDayDownload.cancel()
        return radarDayJob.load()
    }

    private suspend fun renderedPrecipitationOverlay(
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

    private fun onPegmanDropped(location: Location) {
        if (state.value.isPlaying) {
            stopPlaying()
        }
        state.update { state: RadarState ->
            state.copy(droppedLocation = location)
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
        if (layer == RadarLayer.PRECIPITATION) {
            downloadRadarDay()?.join()
        }
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
                RadarLayer.POLLEN -> false
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
            RadarLayer.POLLEN -> 0
        }

    private fun RadarState.index(layer: RadarLayer): Int =
        when (layer) {
            RadarLayer.PRECIPITATION -> precipitationIndex
            RadarLayer.WIND -> windIndex
            RadarLayer.POLLEN -> 0
        }

    private fun RadarState.isFrameMissing(layer: RadarLayer): Boolean =
        frameCount(layer = layer) > 0 &&
            when (layer) {
                RadarLayer.PRECIPITATION -> precipitationOverlay == null
                RadarLayer.WIND -> windField == null
                RadarLayer.POLLEN -> false
            }
}