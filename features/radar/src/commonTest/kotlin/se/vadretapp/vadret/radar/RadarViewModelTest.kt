package se.vadretapp.vadret.radar

import androidx.compose.ui.graphics.ImageBitmap
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.pollen.model.PollenLevel
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences
import se.vadretapp.vadret.pollenseason.ObservePollenInSeasonTask
import se.vadretapp.vadret.radar.model.PollenMap
import se.vadretapp.vadret.radar.model.PollenMarker
import se.vadretapp.vadret.radar.model.PollenStation
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.model.RadarFrame
import se.vadretapp.vadret.radar.model.RadarImage
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.model.WindTimeFrame
import se.vadretapp.vadret.radar.pollen.PollenCloudRenderer
import se.vadretapp.vadret.radar.precipitation.PrecipitationMotion
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
import se.vadretapp.vadret.radar.wind.WindFieldValues
import se.vadretapp.vadret.radar.wind.WindPalette
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.testing.FixedClock
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.network_failure
import se.vadretapp.vadret.ui.resources.no_radar_available
import se.vadretapp.vadret.ui.resources.no_wind_available
import se.vadretapp.vadret.wind.model.WindForecast
import se.vadretapp.vadret.wind.model.WindFrame
import se.vadretapp.vadret.wind.model.WindGrid
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private class FakeObserveRadarTask(
    var result: Either<Failure, Radar>,
    var refreshedResult: Either<Failure, Radar>? = null
) : ObserveRadarTask {

    val requestedImageUrls = mutableListOf<String?>()
    val refreshGate: CompletableDeferred<Unit> = CompletableDeferred()

    override fun invoke(imageUrl: String?): Flow<Either<Failure, Radar>> =
        flow {
            requestedImageUrls.add(imageUrl)
            emit(value = result)
            val refreshed: Either<Failure, Radar>? = refreshedResult
            if (refreshed != null) {
                refreshGate.await()
                emit(value = refreshed)
            }
        }
}

private class FakeRadarImageLoader : RadarImageLoader {

    val loadedUrls = mutableListOf<String>()
    val radarEchoes = mutableMapOf<String, RadarEchoes>()
    var failingUrls: Set<String> = emptySet()
    var loadDuration: Duration = Duration.ZERO

    override suspend fun load(imageUrl: String): Either<Failure, RadarEchoes> {
        loadedUrls.add(imageUrl)
        delay(duration = loadDuration)
        return if (imageUrl in failingUrls) {
            Failure(errorCode = ErrorCode.NETWORK_FAILURE).asLeft()
        } else {
            radarEchoes.getOrPut(imageUrl) { fakeRadarEchoes() }.asRight()
        }
    }
}

private class FakeDownloadRadarDayTask : DownloadRadarDayTask {

    val requestedImageUrls = mutableListOf<List<String>>()
    var result: Either<Failure, Unit> = Unit.asRight()
    var downloadDuration: Duration = Duration.ZERO

    override suspend fun invoke(imageUrls: List<String>): Either<Failure, Unit> {
        requestedImageUrls.add(imageUrls)
        delay(duration = downloadDuration)
        return result
    }
}

private class FakePrecipitationRenderer : PrecipitationRenderer {

    override suspend fun render(echoes: RadarEchoes): PrecipitationOverlay =
        PrecipitationOverlay.Echoes(
            echoes = echoes.image,
            motion = null
        )

    override suspend fun renderMotion(
        echoes: RadarEchoes,
        nextEchoes: RadarEchoes,
        duration: Duration
    ): PrecipitationOverlay =
        PrecipitationOverlay.Echoes(
            echoes = echoes.image,
            motion = PrecipitationMotion(
                nextEchoes = nextEchoes.image,
                field = FakeImageBitmap(),
                duration = duration
            )
        )
}

private val RadarState.shownEchoes: ImageBitmap?
    get() = (precipitationOverlay as? PrecipitationOverlay.Echoes)?.echoes

private val RadarState.shownMotion: PrecipitationMotion?
    get() = (precipitationOverlay as? PrecipitationOverlay.Echoes)?.motion

private class FakeObserveWindForecastTask(
    var result: Either<Failure, WindForecast>,
    var refreshedResult: Either<Failure, WindForecast>? = null
) : ObserveWindForecastTask {

    var requestCount: Int = 0
    val refreshGate: CompletableDeferred<Unit> = CompletableDeferred()

    override fun invoke(): Flow<Either<Failure, WindForecast>> =
        flow {
            requestCount++
            emit(value = result)
            val refreshed: Either<Failure, WindForecast>? = refreshedResult
            if (refreshed != null) {
                refreshGate.await()
                emit(value = refreshed)
            }
        }
}

private class FakeGetPollenMapTask(
    var result: Either<Failure, PollenMap>
) : GetPollenMapTask {

    var requestCount: Int = 0
    val requestedDates = mutableListOf<LocalDate>()

    override suspend fun invoke(date: LocalDate): Either<Failure, PollenMap> {
        requestCount++
        requestedDates.add(date)
        return result
    }
}

private val defaultPollenPreferences = PollenPreferences(
    isPollenEnabled = true,
    allergenPollenIds = emptySet(),
    isHibernationNoticeShown = false,
    isEndOfSeasonSimulated = false
)

private class FakeObservePollenInSeasonTask : ObservePollenInSeasonTask {

    val isInSeason = MutableStateFlow<Either<Failure, Boolean>>(value = true.asRight())

    override fun invoke(): Flow<Either<Failure, Boolean>> = isInSeason
}

private class RecordingSetPollenHibernationNoticeShownTask : SetPollenHibernationNoticeShownTask {

    val shownValues = mutableListOf<Boolean>()

    override suspend fun invoke(isShown: Boolean): Either<Failure, Unit> {
        shownValues.add(isShown)
        return Unit.asRight()
    }
}

private class FakeObservePollenPreferencesTask : ObservePollenPreferencesTask {

    val pollenPreferences = MutableStateFlow<Either<Failure, PollenPreferences>>(value = defaultPollenPreferences.asRight())

    override fun invoke(): Flow<Either<Failure, PollenPreferences>> = pollenPreferences
}

private class FakePollenCloudRenderer : PollenCloudRenderer {

    val renderedMarkers = mutableListOf<List<PollenMarker>>()
    val pollenClouds = FakeImageBitmap()

    override suspend fun render(pollenMarkers: List<PollenMarker>): ImageBitmap {
        renderedMarkers.add(pollenMarkers)
        return pollenClouds
    }
}

private class FakeGetDeviceLocationTask(
    var result: Either<Failure, Location>
) : GetDeviceLocationTask {
    override suspend fun invoke(): Either<Failure, Location> = result
}

private class FakeWindFieldRenderer : WindFieldRenderer {

    val renderedFrames = mutableListOf<WindFrame>()
    val renderedPalettes = mutableListOf<WindPalette>()
    val blendedFields = mutableMapOf<Float, WindField>()

    override suspend fun render(
        windGrid: WindGrid,
        windFrame: WindFrame,
        windPalette: WindPalette
    ): WindField {
        renderedFrames.add(windFrame)
        renderedPalettes.add(windPalette)
        return WindField(
            values = WindFieldValues(
                width = 1,
                height = 1,
                eastward = floatArrayOf(0f),
                northward = floatArrayOf(1f),
                speeds = floatArrayOf(1f),
                colors = intArrayOf(0)
            ),
            image = FakeImageBitmap()
        )
    }

    override suspend fun blend(
        windField: WindField,
        nextWindField: WindField,
        progress: Float
    ): WindField =
        blendedFields.getOrPut(progress) {
            WindField(
                values = windField.values,
                image = FakeImageBitmap()
            )
        }
}

private val FETCHED_AT: Instant = Instant.parse(input = "2026-10-24T12:40:00Z")

private val windForecast = WindForecast(
    referenceTime = Instant.parse(input = "2026-10-24T09:30:00Z"),
    grid = WindGrid(
        columnCount = 1,
        rowCount = 1,
        longitudes = listOf(18.0),
        latitudes = listOf(59.3)
    ),
    frames = (10..12).map { hour: Int ->
        WindFrame(
            validAt = Instant.parse(input = "2026-10-24T$hour:00:00Z"),
            windSpeeds = listOf(hour.toFloat()),
            windFromDirections = listOf(270f)
        )
    },
    fetchedAt = FETCHED_AT
)

private val birch = PollenType(
    pollenId = "birch",
    name = "Björk",
    hasForecasts = true
)

private val grass = PollenType(
    pollenId = "grass",
    name = "Gräs",
    hasForecasts = true
)

private val pollenMap = PollenMap(
    date = LocalDate.parse(input = "2026-07-13"),
    pollenTypes = listOf(birch, grass),
    stations = listOf(
        PollenStation(
            regionId = "stockholm",
            name = "Stockholm",
            latitude = 59.37,
            longitude = 18.06,
            pollenLevels = mapOf(
                "birch" to PollenLevel.LOW,
                "grass" to PollenLevel.HIGH
            )
        ),
        PollenStation(
            regionId = "kiruna",
            name = "Kiruna",
            latitude = 67.85,
            longitude = 20.3,
            pollenLevels = mapOf("grass" to PollenLevel.MODERATE)
        )
    )
)

private val radar = Radar(
    images = (0..2).map { minute: Int ->
        RadarImage(
            imageUrl = "https://smhi.se/$minute.png",
            validAt = Instant.parse(input = "2026-10-24T12:0$minute:00Z")
        )
    },
    fetchedAt = FETCHED_AT
)

private val newerRadar = Radar(
    images = (0..3).map { minute: Int ->
        RadarImage(
            imageUrl = "https://smhi.se/$minute.png",
            validAt = Instant.parse(input = "2026-10-24T12:0$minute:00Z")
        )
    },
    fetchedAt = FETCHED_AT
)

private fun RadarState.shownImageUrl(): String? = precipitationFrames.getOrNull(index = precipitationIndex)?.imageUrl

@OptIn(ExperimentalCoroutinesApi::class)
class RadarViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val getRadarTask = FakeObserveRadarTask(result = radar.asRight())
    private val getWindForecastTask = FakeObserveWindForecastTask(result = windForecast.asRight())
    private val getPollenMapTask = FakeGetPollenMapTask(result = pollenMap.asRight())
    private val pollenCloudRenderer = FakePollenCloudRenderer()
    private val getDeviceLocationTask = FakeGetDeviceLocationTask(result = Location(latitude = 59.3, longitude = 18.0).asRight())
    private val radarImageLoader = FakeRadarImageLoader()
    private val downloadRadarDayTask = FakeDownloadRadarDayTask()
    private val windFieldRenderer = FakeWindFieldRenderer()
    private val observePollenPreferencesTask = FakeObservePollenPreferencesTask()
    private val observePollenInSeasonTask = FakeObservePollenInSeasonTask()
    private val setPollenHibernationNoticeShownTask = RecordingSetPollenHibernationNoticeShownTask()
    private val clock = FixedClock(now = Instant.parse(input = "2026-10-24T12:45:00Z"))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        isStarted: Boolean = true,
        initialImageUrl: String? = null
    ): RadarViewModel {
        val radarViewModel = RadarViewModel(
            observeRadarTask = getRadarTask,
            downloadRadarDayTask = downloadRadarDayTask,
            observeWindForecastTask = getWindForecastTask,
            getDeviceLocationTask = getDeviceLocationTask,
            getPollenMapTask = getPollenMapTask,
            observePollenPreferencesTask = observePollenPreferencesTask,
            observePollenInSeasonTask = observePollenInSeasonTask,
            setPollenHibernationNoticeShownTask = setPollenHibernationNoticeShownTask,
            radarImageLoader = radarImageLoader,
            precipitationRenderer = FakePrecipitationRenderer(),
            windFieldRenderer = windFieldRenderer,
            pollenCloudRenderer = pollenCloudRenderer,
            clock = clock,
            coroutineDispatcher = testDispatcher
        )
        if (isStarted) {
            radarViewModel.onEvent(event = RadarEvent.OnStart(initialImageUrl = initialImageUrl))
        }
        return radarViewModel
    }

    @Test
    fun `a dropped pegman is kept until its island is closed`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            val kiruna = Location(latitude = 67.8558, longitude = 20.2253)

            radarViewModel.onEvent(event = RadarEvent.OnPegmanDropped(location = kiruna))
            val dropped: Location? = radarViewModel.state.value.droppedLocation
            radarViewModel.onEvent(event = RadarEvent.OnPlaceIslandDismissed)

            assertEquals(expected = kiruna, actual = dropped)
            assertNull(actual = radarViewModel.state.value.droppedLocation)
        }

    @Test
    fun `dropping the pegman stops the radar from playing`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceTimeBy(delayTimeMillis = 128)
            runCurrent()

            radarViewModel.onEvent(event = RadarEvent.OnPegmanDropped(location = Location(latitude = 67.8558, longitude = 20.2253)))
            val stoppedIndex: Int = radarViewModel.state.value.precipitationIndex
            advanceTimeBy(delayTimeMillis = 512)
            runCurrent()

            assertEquals(expected = false, actual = radarViewModel.state.value.isPlaying)
            assertEquals(expected = stoppedIndex, actual = radarViewModel.state.value.precipitationIndex)
        }

    @Test
    fun `dropping the pegman stops the wind from playing`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceTimeBy(delayTimeMillis = 128)
            runCurrent()

            radarViewModel.onEvent(event = RadarEvent.OnPegmanDropped(location = Location(latitude = 67.8558, longitude = 20.2253)))
            val stoppedIndex: Int = radarViewModel.state.value.windIndex
            advanceTimeBy(delayTimeMillis = 512)
            runCurrent()

            assertEquals(expected = false, actual = radarViewModel.state.value.isPlaying)
            assertEquals(expected = stoppedIndex, actual = radarViewModel.state.value.windIndex)
        }

    @Test
    fun `dropping the pegman again moves it`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            val malmo = Location(latitude = 55.6050, longitude = 13.0038)

            radarViewModel.onEvent(event = RadarEvent.OnPegmanDropped(location = Location(latitude = 67.8558, longitude = 20.2253)))
            radarViewModel.onEvent(event = RadarEvent.OnPegmanDropped(location = malmo))

            assertEquals(expected = malmo, actual = radarViewModel.state.value.droppedLocation)
        }

    @Test
    fun `the newest frame is shown once loaded`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            val state: RadarState = radarViewModel.state.value
            assertEquals(expected = 3, actual = state.precipitationFrames.size)
            assertEquals(expected = 2, actual = state.precipitationIndex)
            assertSame(expected = radarImageLoader.radarEchoes["https://smhi.se/2.png"]?.image, actual = state.shownEchoes)
            assertNull(actual = state.shownMotion)
            assertEquals(expected = false, actual = state.isLoading)
        }

    @Test
    fun `a requested image is asked for and shown first`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel(initialImageUrl = "https://smhi.se/1.png")
            advanceUntilIdle()

            val state: RadarState = radarViewModel.state.value
            assertEquals(expected = listOf<String?>("https://smhi.se/1.png"), actual = getRadarTask.requestedImageUrls)
            assertEquals(expected = 1, actual = state.precipitationIndex)
            assertSame(expected = radarImageLoader.radarEchoes["https://smhi.se/1.png"]?.image, actual = state.shownEchoes)
        }

    @Test
    fun `a requested image that the radar lacks shows the newest frame`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel(initialImageUrl = "https://smhi.se/9.png")
            advanceUntilIdle()

            assertEquals(expected = 2, actual = radarViewModel.state.value.precipitationIndex)
        }

    @Test
    fun `a radar that cannot be loaded shows its message`() =
        testScope.runTest {
            getRadarTask.result = Failure(errorCode = ErrorCode.RADAR_NOT_AVAILABLE).asLeft()

            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(expected = Res.string.no_radar_available, actual = radarViewModel.state.value.errorMessageResource)
            assertEquals(expected = false, actual = radarViewModel.state.value.isLoading)
            radarViewModel.onEvent(event = RadarEvent.OnErrorShown)
            assertNull(actual = radarViewModel.state.value.errorMessageResource)
        }

    @Test
    fun `playing steps through the frames and wraps around`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceTimeBy(delayTimeMillis = 128)
            runCurrent()
            val firstIndex: Int = radarViewModel.state.value.precipitationIndex
            advanceTimeBy(delayTimeMillis = 128)
            runCurrent()
            val secondIndex: Int = radarViewModel.state.value.precipitationIndex
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceUntilIdle()

            assertEquals(expected = listOf(0, 1), actual = listOf(firstIndex, secondIndex))
            assertEquals(expected = false, actual = radarViewModel.state.value.isPlaying)
        }

    @Test
    fun `a frame that fails to load stops playback`() =
        testScope.runTest {
            radarImageLoader.failingUrls = setOf("https://smhi.se/0.png")
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceUntilIdle()

            assertEquals(expected = false, actual = radarViewModel.state.value.isPlaying)
            assertEquals(expected = Res.string.network_failure, actual = radarViewModel.state.value.errorMessageResource)
        }

    @Test
    fun `dragging the slider pauses and shows that frame`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)

            radarViewModel.onEvent(event = RadarEvent.OnIndexChanged(index = 1))
            advanceUntilIdle()

            val state: RadarState = radarViewModel.state.value
            assertEquals(expected = false, actual = state.isPlaying)
            assertEquals(expected = 1, actual = state.precipitationIndex)
            assertSame(expected = radarImageLoader.radarEchoes["https://smhi.se/1.png"]?.image, actual = state.shownEchoes)
            assertNull(actual = state.shownMotion)
        }

    @Test
    fun `a frame that is slow to load shows that the radar is loading`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            radarViewModel.onEvent(event = RadarEvent.OnMapLoaded)
            advanceUntilIdle()
            val shownState: RadarState = radarViewModel.state.value
            radarImageLoader.loadDuration = 2.seconds

            radarViewModel.onEvent(event = RadarEvent.OnIndexChanged(index = 1))
            advanceTimeBy(delayTime = 1.seconds)
            val waitingState: RadarState = radarViewModel.state.value
            advanceUntilIdle()

            assertEquals(expected = false, actual = shownState.isProgressShown)
            assertEquals(expected = true, actual = waitingState.isProgressShown)
            assertEquals(expected = false, actual = radarViewModel.state.value.isProgressShown)
            assertEquals(expected = 1, actual = radarViewModel.state.value.precipitationIndex)
        }

    @Test
    fun `a frame that loads quickly does not flash the loading bar`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarImageLoader.loadDuration = 200.milliseconds

            radarViewModel.onEvent(event = RadarEvent.OnIndexChanged(index = 1))
            advanceTimeBy(delayTime = 1.seconds)

            assertEquals(expected = false, actual = radarViewModel.state.value.isWaitingForFrame)
            assertEquals(expected = 1, actual = radarViewModel.state.value.precipitationIndex)
        }

    @Test
    fun `playing moves each frame towards the one after it`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceTimeBy(delayTimeMillis = 128)
            runCurrent()
            val motion: PrecipitationMotion? = radarViewModel.state.value.shownMotion
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)

            assertSame(expected = radarImageLoader.radarEchoes["https://smhi.se/1.png"]?.image, actual = motion?.nextEchoes)
            assertEquals(expected = 128.milliseconds, actual = motion?.duration)
        }

    @Test
    fun `opening the radar loads only the frame it shows`() =
        testScope.runTest {
            viewModel()
            advanceUntilIdle()

            assertEquals(expected = listOf("https://smhi.se/2.png"), actual = radarImageLoader.loadedUrls)
            assertEquals(expected = emptyList(), actual = downloadRadarDayTask.requestedImageUrls)
        }

    @Test
    fun `playing downloads the day before it loads the frames`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            radarViewModel.onEvent(event = RadarEvent.OnMapLoaded)
            advanceUntilIdle()
            downloadRadarDayTask.downloadDuration = 2.seconds

            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceTimeBy(delayTime = 1.seconds)
            val downloadingState: RadarState = radarViewModel.state.value
            val loadedWhileDownloading: List<String> = radarImageLoader.loadedUrls.toList()
            advanceTimeBy(delayTime = 2.seconds)
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)

            assertEquals(
                expected = listOf(listOf("https://smhi.se/0.png", "https://smhi.se/1.png", "https://smhi.se/2.png")),
                actual = downloadRadarDayTask.requestedImageUrls
            )
            assertEquals(expected = true, actual = downloadingState.isProgressShown)
            assertEquals(expected = listOf("https://smhi.se/2.png"), actual = loadedWhileDownloading)
            assertEquals(expected = true, actual = "https://smhi.se/0.png" in radarImageLoader.loadedUrls)
            assertEquals(expected = false, actual = radarViewModel.state.value.isProgressShown)
        }

    @Test
    fun `the day is downloaded once however often the slider moves and the radar plays`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            downloadRadarDayTask.downloadDuration = 2.seconds

            radarViewModel.onEvent(event = RadarEvent.OnIndexChanged(index = 0))
            radarViewModel.onEvent(event = RadarEvent.OnIndexChanged(index = 1))
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceTimeBy(delayTimeMillis = 128)
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceUntilIdle()

            assertEquals(expected = 1, actual = downloadRadarDayTask.requestedImageUrls.size)
        }

    @Test
    fun `a day that fails to download still plays its frames`() =
        testScope.runTest {
            downloadRadarDayTask.result = Failure(errorCode = ErrorCode.NETWORK_FAILURE).asLeft()
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceTimeBy(delayTimeMillis = 128)
            runCurrent()
            val state: RadarState = radarViewModel.state.value
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)

            assertEquals(expected = true, actual = state.isPlaying)
            assertEquals(expected = 0, actual = state.precipitationIndex)
            assertNull(actual = state.errorMessageResource)
        }

    @Test
    fun `playing loads the frames a few steps ahead`() =
        testScope.runTest {
            getRadarTask.result = Radar(
                images = (0..5).map { minute: Int ->
                    RadarImage(
                        imageUrl = "https://smhi.se/$minute.png",
                        validAt = Instant.parse(input = "2026-10-24T12:0$minute:00Z")
                    )
                },
                fetchedAt = FETCHED_AT
            ).asRight()
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            runCurrent()
            val loadedUrls: List<String> = radarImageLoader.loadedUrls.toList()
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)

            assertEquals(expected = true, actual = "https://smhi.se/3.png" in loadedUrls)
            assertEquals(expected = false, actual = "https://smhi.se/4.png" in loadedUrls)
        }

    @Test
    fun `the newest frame does not move on to the oldest`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceTimeBy(delayTimeMillis = 128 * 3)
            runCurrent()
            val state: RadarState = radarViewModel.state.value
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)

            assertEquals(expected = 2, actual = state.precipitationIndex)
            assertNull(actual = state.shownMotion)
        }

    @Test
    fun `leaving the radar stops playing and keeps the frame`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceTimeBy(delayTimeMillis = 128)
            runCurrent()

            radarViewModel.onEvent(event = RadarEvent.OnLeft)
            advanceTimeBy(delayTimeMillis = 1_000)
            runCurrent()

            val state: RadarState = radarViewModel.state.value
            assertEquals(expected = false, actual = state.isPlaying)
            assertEquals(expected = 0, actual = state.precipitationIndex)
        }

    @Test
    fun `pausing keeps the frame still`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceTimeBy(delayTimeMillis = 128)
            runCurrent()

            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)

            val state: RadarState = radarViewModel.state.value
            assertEquals(expected = 0, actual = state.precipitationIndex)
            assertSame(expected = radarImageLoader.radarEchoes["https://smhi.se/0.png"]?.image, actual = state.shownEchoes)
            assertNull(actual = state.shownMotion)
        }

    @Test
    fun `a next frame that cannot be loaded keeps the frame still while playing`() =
        testScope.runTest {
            radarImageLoader.failingUrls = setOf("https://smhi.se/1.png")
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceTimeBy(delayTimeMillis = 128)
            runCurrent()
            val state: RadarState = radarViewModel.state.value
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)

            assertEquals(expected = true, actual = state.isPlaying)
            assertSame(expected = radarImageLoader.radarEchoes["https://smhi.se/0.png"]?.image, actual = state.shownEchoes)
            assertNull(actual = state.shownMotion)
        }

    @Test
    fun `nothing loads before the screen starts the view model`() =
        testScope.runTest {
            viewModel(isStarted = false)
            advanceUntilIdle()

            assertEquals(expected = emptyList(), actual = radarImageLoader.loadedUrls)
            assertEquals(expected = 0, actual = getWindForecastTask.requestCount)
        }

    @Test
    fun `the device location is shown when known`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(expected = Location(latitude = 59.3, longitude = 18.0), actual = radarViewModel.state.value.deviceLocation)
        }

    @Test
    fun `an unknown device location shows no marker and no message`() =
        testScope.runTest {
            getDeviceLocationTask.result = Failure(errorCode = ErrorCode.LOCATION_PERMISSION_MISSING).asLeft()

            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            assertNull(actual = radarViewModel.state.value.deviceLocation)
            assertNull(actual = radarViewModel.state.value.errorMessageResource)
        }

    @Test
    fun `choosing wind loads the forecast once and shows the first hour`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.PRECIPITATION))
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()

            val state: RadarState = radarViewModel.state.value
            assertEquals(expected = RadarLayer.WIND, actual = state.layer)
            assertEquals(expected = 1, actual = getWindForecastTask.requestCount)
            assertEquals(expected = 3, actual = state.windFrames.size)
            assertEquals(expected = 0, actual = state.windIndex)
            assertEquals(expected = windForecast.frames.first(), actual = windFieldRenderer.renderedFrames.first())
            assertEquals(expected = false, actual = state.isLoading)
        }

    @Test
    fun `the dark theme colours the wind field with the night palette`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            radarViewModel.onEvent(event = RadarEvent.OnDarkThemeChanged(isDarkTheme = true))
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()

            assertEquals(expected = listOf(WindPalette.NIGHT), actual = windFieldRenderer.renderedPalettes)
        }

    @Test
    fun `switching theme repaints the shown wind field only when the palette changes`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnDarkThemeChanged(isDarkTheme = false))
            radarViewModel.onEvent(event = RadarEvent.OnDarkThemeChanged(isDarkTheme = true))
            advanceUntilIdle()

            assertEquals(
                expected = listOf(WindPalette.DAY, WindPalette.NIGHT),
                actual = windFieldRenderer.renderedPalettes
            )
            assertEquals(expected = windForecast.frames.first(), actual = windFieldRenderer.renderedFrames.last())
        }

    @Test
    fun `a wind forecast that fails shows its message and is asked again next time`() =
        testScope.runTest {
            getWindForecastTask.result = Failure(errorCode = ErrorCode.WIND_NOT_AVAILABLE).asLeft()
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()
            assertEquals(expected = Res.string.no_wind_available, actual = radarViewModel.state.value.errorMessageResource)
            assertEquals(expected = false, actual = radarViewModel.state.value.isLoading)

            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.PRECIPITATION))
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()
            assertEquals(expected = 2, actual = getWindForecastTask.requestCount)
        }

    @Test
    fun `choosing pollen loads the map once and shows the strongest type`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            radarViewModel.onEvent(event = RadarEvent.OnLocaleChanged(languageTag = "en", hourFormat = HourFormat.TWENTY_FOUR_HOUR))
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.POLLEN))
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.PRECIPITATION))
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.POLLEN))
            advanceUntilIdle()

            val state: RadarState = radarViewModel.state.value
            assertEquals(
                expected = listOf(clock.todayIn(timeZone = TimeZone.currentSystemDefault())),
                actual = getPollenMapTask.requestedDates
            )
            assertEquals(expected = listOf(birch, grass), actual = state.pollenTypes)
            assertEquals(expected = "grass", actual = state.selectedPollenId)
            assertEquals(
                expected = listOf(
                    PollenMarker(
                        latitude = 59.37,
                        longitude = 18.06,
                        pollenLevel = PollenLevel.HIGH
                    ),
                    PollenMarker(
                        latitude = 67.85,
                        longitude = 20.3,
                        pollenLevel = PollenLevel.MODERATE
                    )
                ),
                actual = state.pollenMarkers
            )
            assertEquals(expected = "Monday", actual = state.pollenDayText)
            assertEquals(expected = "July 13, 2026", actual = state.pollenDateText)
            assertEquals(expected = false, actual = state.isLoading)
        }

    @Test
    fun `choosing another pollen type shows it at the stations that forecast it`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.POLLEN))
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnPollenTypeSelected(pollenId = "birch"))
            advanceUntilIdle()

            assertEquals(expected = "birch", actual = radarViewModel.state.value.selectedPollenId)
            assertEquals(
                expected = listOf(
                    PollenMarker(
                        latitude = 59.37,
                        longitude = 18.06,
                        pollenLevel = PollenLevel.LOW
                    )
                ),
                actual = radarViewModel.state.value.pollenMarkers
            )
        }

    @Test
    fun `pollen clouds are drawn for the shown type and again for the next`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.POLLEN))
            advanceUntilIdle()
            val grassMarkers: List<PollenMarker> = radarViewModel.state.value.pollenMarkers

            radarViewModel.onEvent(event = RadarEvent.OnPollenTypeSelected(pollenId = "birch"))
            advanceUntilIdle()

            assertEquals(
                expected = listOf(grassMarkers, radarViewModel.state.value.pollenMarkers),
                actual = pollenCloudRenderer.renderedMarkers
            )
            assertSame(expected = pollenCloudRenderer.pollenClouds, actual = radarViewModel.state.value.pollenClouds)
        }

    @Test
    fun `a new language rewrites the pollen day`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            radarViewModel.onEvent(event = RadarEvent.OnLocaleChanged(languageTag = "en", hourFormat = HourFormat.TWENTY_FOUR_HOUR))
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.POLLEN))
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnLocaleChanged(languageTag = "sv-SE", hourFormat = HourFormat.TWENTY_FOUR_HOUR))
            advanceUntilIdle()

            assertEquals(expected = "Måndag", actual = radarViewModel.state.value.pollenDayText)
            assertEquals(expected = "13 juli 2026", actual = radarViewModel.state.value.pollenDateText)
        }

    @Test
    fun `a pollen map that fails shows its message and is asked again next time`() =
        testScope.runTest {
            getPollenMapTask.result = Failure(errorCode = ErrorCode.NETWORK_FAILURE).asLeft()
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.POLLEN))
            advanceUntilIdle()
            assertEquals(expected = Res.string.network_failure, actual = radarViewModel.state.value.errorMessageResource)
            assertEquals(expected = false, actual = radarViewModel.state.value.isLoading)
            assertEquals(expected = emptyList(), actual = radarViewModel.state.value.pollenMarkers)

            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.PRECIPITATION))
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.POLLEN))
            advanceUntilIdle()
            assertEquals(expected = 2, actual = getPollenMapTask.requestCount)
        }

    @Test
    fun `the pollen layer has nothing to play or scrub`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.POLLEN))
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnIndexChanged(index = 0))
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceUntilIdle()

            assertEquals(expected = false, actual = radarViewModel.state.value.isPlaying)
            assertEquals(expected = 2, actual = radarViewModel.state.value.precipitationIndex)
        }

    @Test
    fun `the slider on the wind layer renders that hour`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnIndexChanged(index = 2))
            advanceUntilIdle()

            assertEquals(expected = 2, actual = radarViewModel.state.value.windIndex)
            assertEquals(expected = windForecast.frames[2], actual = windFieldRenderer.renderedFrames.last())
            assertEquals(expected = 2, actual = radarViewModel.state.value.precipitationIndex)
        }

    @Test
    fun `playing the wind blends towards the next hour`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceTimeBy(delayTimeMillis = 64)
            runCurrent()
            val state: RadarState = radarViewModel.state.value
            val renderedFrames: List<WindFrame> = windFieldRenderer.renderedFrames.toList()
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)

            assertEquals(expected = 0, actual = state.windIndex)
            assertSame(expected = windFieldRenderer.blendedFields[0.5f], actual = state.shownWindField)
            assertEquals(expected = listOf(windForecast.frames[1], windForecast.frames[2]), actual = renderedFrames.takeLast(n = 2))
        }

    @Test
    fun `pausing the wind shows the hour on the slider`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceTimeBy(delayTimeMillis = 64)
            runCurrent()

            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)

            val state: RadarState = radarViewModel.state.value
            assertNull(actual = state.windTransitionField)
            assertSame(expected = state.windField, actual = state.shownWindField)
        }

    @Test
    fun `playing the wind steps hour by hour`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceTimeBy(delayTimeMillis = 128)
            runCurrent()
            val windIndex: Int = radarViewModel.state.value.windIndex
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)
            advanceUntilIdle()

            assertEquals(expected = 1, actual = windIndex)
        }

    @Test
    fun `switching layer stops playing`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnPlayButtonClicked)

            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()

            assertEquals(expected = false, actual = radarViewModel.state.value.isPlaying)
        }

    @Test
    fun `coming back to precipitation keeps the chosen frame`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnIndexChanged(index = 0))
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.PRECIPITATION))
            advanceUntilIdle()

            assertEquals(expected = 0, actual = radarViewModel.state.value.precipitationIndex)
        }

    @Test
    fun `choosing wind before the newest image loads uncovers the map with the first hour`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            radarViewModel.onEvent(event = RadarEvent.OnMapLoaded)

            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()

            val state: RadarState = radarViewModel.state.value
            assertEquals(expected = false, actual = state.isInitialLoading)
            assertEquals(expected = false, actual = state.isLoading)
            assertEquals(expected = windForecast.frames.first(), actual = windFieldRenderer.renderedFrames.single())
            assertNull(actual = state.precipitationOverlay)
        }

    @Test
    fun `coming back to precipitation shows the newest image skipped for wind`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            radarViewModel.onEvent(event = RadarEvent.OnMapLoaded)
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.PRECIPITATION))
            advanceUntilIdle()

            val state: RadarState = radarViewModel.state.value
            assertEquals(expected = radarImageLoader.radarEchoes.getValue(key = "https://smhi.se/2.png").image, actual = state.shownEchoes)
            assertEquals(expected = 2, actual = state.precipitationIndex)
            assertEquals(expected = false, actual = state.isInitialLoading)
        }

    @Test
    fun `a forecast that arrives after leaving wind leaves the radar alone and shows on coming back`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            val shownEchoes: ImageBitmap? = radarViewModel.state.value.shownEchoes

            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.PRECIPITATION))
            advanceUntilIdle()
            val onPrecipitation: RadarState = radarViewModel.state.value
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()

            assertSame(expected = shownEchoes, actual = onPrecipitation.shownEchoes)
            assertNull(actual = onPrecipitation.windField)
            assertEquals(expected = false, actual = onPrecipitation.isLoading)
            assertEquals(expected = windForecast.frames.first(), actual = windFieldRenderer.renderedFrames.single())
            assertEquals(expected = 1, actual = getWindForecastTask.requestCount)
        }

    @Test
    fun `a wind forecast that fails before the radar loads stops loading`() =
        testScope.runTest {
            getWindForecastTask.result = Failure(errorCode = ErrorCode.NETWORK_FAILURE).asLeft()
            val radarViewModel: RadarViewModel = viewModel(isStarted = false)
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnStart(initialImageUrl = null))
            advanceUntilIdle()

            assertEquals(expected = false, actual = radarViewModel.state.value.isLoading)
        }

    @Test
    fun `a new language rewrites the weekdays and keeps the chosen frame`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel(isStarted = false)
            radarViewModel.onEvent(event = RadarEvent.OnLocaleChanged(languageTag = "en", hourFormat = HourFormat.TWENTY_FOUR_HOUR))
            radarViewModel.onEvent(event = RadarEvent.OnStart(initialImageUrl = null))
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnIndexChanged(index = 1))
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()
            val english: RadarState = radarViewModel.state.value

            radarViewModel.onEvent(event = RadarEvent.OnLocaleChanged(languageTag = "sv-SE", hourFormat = HourFormat.TWENTY_FOUR_HOUR))
            advanceUntilIdle()
            val swedish: RadarState = radarViewModel.state.value

            assertEquals(expected = listOf("Saturday"), actual = english.precipitationFrames.map { frame: RadarFrame -> frame.weekday }.distinct())
            assertEquals(expected = listOf("Saturday"), actual = english.windFrames.map { frame: WindTimeFrame -> frame.weekday }.distinct())
            assertEquals(expected = listOf("lördag"), actual = swedish.precipitationFrames.map { frame: RadarFrame -> frame.weekday }.distinct())
            assertEquals(expected = listOf("lördag"), actual = swedish.windFrames.map { frame: WindTimeFrame -> frame.weekday }.distinct())
            assertEquals(expected = 1, actual = swedish.precipitationIndex)
        }

    @Test
    fun `the same language again leaves the state alone`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel(isStarted = false)
            radarViewModel.onEvent(event = RadarEvent.OnLocaleChanged(languageTag = "en", hourFormat = HourFormat.TWENTY_FOUR_HOUR))
            radarViewModel.onEvent(event = RadarEvent.OnStart(initialImageUrl = null))
            advanceUntilIdle()
            val before: RadarState = radarViewModel.state.value

            radarViewModel.onEvent(event = RadarEvent.OnLocaleChanged(languageTag = "en", hourFormat = HourFormat.TWENTY_FOUR_HOUR))
            advanceUntilIdle()

            assertSame(expected = before, actual = radarViewModel.state.value)
        }

    @Test
    fun `a language chosen before anything loaded is used once it loads`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel(isStarted = false)
            radarViewModel.onEvent(event = RadarEvent.OnLocaleChanged(languageTag = "sv-SE", hourFormat = HourFormat.TWENTY_FOUR_HOUR))
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnStart(initialImageUrl = null))
            advanceUntilIdle()

            assertEquals(expected = listOf("lördag"), actual = radarViewModel.state.value.precipitationFrames.map { frame: RadarFrame -> frame.weekday }.distinct())
        }

    @Test
    fun `the map stays covered until both the map and the newest image are ready`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            val beforeAnything: Boolean = radarViewModel.state.value.isInitialLoading
            advanceUntilIdle()
            val imageWithoutMap: Boolean = radarViewModel.state.value.isInitialLoading

            radarViewModel.onEvent(event = RadarEvent.OnMapLoaded)

            assertEquals(expected = true, actual = beforeAnything)
            assertEquals(expected = true, actual = imageWithoutMap)
            assertEquals(expected = false, actual = radarViewModel.state.value.isInitialLoading)
        }

    @Test
    fun `a map that loads first waits for the newest image`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel(isStarted = false)
            radarViewModel.onEvent(event = RadarEvent.OnMapLoaded)
            val mapWithoutImage: Boolean = radarViewModel.state.value.isInitialLoading

            radarViewModel.onEvent(event = RadarEvent.OnStart(initialImageUrl = null))
            advanceUntilIdle()

            assertEquals(expected = true, actual = mapWithoutImage)
            assertEquals(expected = false, actual = radarViewModel.state.value.isInitialLoading)
        }

    @Test
    fun `a radar or image that cannot be loaded uncovers the map`() =
        testScope.runTest {
            val failures: List<() -> Unit> = listOf(
                { getRadarTask.result = Failure(errorCode = ErrorCode.RADAR_NOT_AVAILABLE).asLeft() },
                {
                    getRadarTask.result = radar.asRight()
                    radarImageLoader.failingUrls = setOf("https://smhi.se/2.png")
                },
                {
                    getRadarTask.result = Radar(
                        images = emptyList(),
                        fetchedAt = FETCHED_AT
                    ).asRight()
                }
            )

            val covered: List<Boolean> = failures.map { fail: () -> Unit ->
                fail()
                val radarViewModel: RadarViewModel = viewModel()
                radarViewModel.onEvent(event = RadarEvent.OnMapLoaded)
                advanceUntilIdle()
                radarViewModel.state.value.isInitialLoading
            }

            assertEquals(expected = List(size = failures.size) { false }, actual = covered)
        }

    @Test
    fun `a refreshed radar moves on to its newest image when the newest one was shown`() =
        testScope.runTest {
            getRadarTask.refreshedResult = newerRadar.asRight()
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            val beforeRefresh: String? = radarViewModel.state.value.shownImageUrl()

            getRadarTask.refreshGate.complete(value = Unit)
            advanceUntilIdle()

            assertEquals(
                expected = "https://smhi.se/2.png" to "https://smhi.se/3.png",
                actual = beforeRefresh to radarViewModel.state.value.shownImageUrl()
            )
            assertEquals(expected = "https://smhi.se/3.png", actual = radarImageLoader.loadedUrls.last())
        }

    @Test
    fun `a refreshed radar keeps an earlier image that was picked`() =
        testScope.runTest {
            getRadarTask.refreshedResult = newerRadar.asRight()
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnIndexChanged(index = 0))
            advanceUntilIdle()

            getRadarTask.refreshGate.complete(value = Unit)
            advanceUntilIdle()

            assertEquals(
                expected = "https://smhi.se/0.png" to 4,
                actual = radarViewModel.state.value.shownImageUrl() to radarViewModel.state.value.precipitationFrames.size
            )
        }

    @Test
    fun `refreshed wind keeps the hour that was picked`() =
        testScope.runTest {
            getWindForecastTask.refreshedResult = windForecast.asRight()
            val radarViewModel: RadarViewModel = viewModel()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnIndexChanged(index = 1))
            advanceUntilIdle()

            getWindForecastTask.refreshGate.complete(value = Unit)
            advanceUntilIdle()

            assertEquals(
                expected = 1 to false,
                actual = radarViewModel.state.value.windIndex to radarViewModel.state.value.isLoading
            )
        }

    @Test
    fun `the dock waits for the map and the newest image so play works when it shows`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            val beforeAnything: Boolean = radarViewModel.state.value.isDockReady
            advanceUntilIdle()
            val imageWithoutMap: Boolean = radarViewModel.state.value.isDockReady

            radarViewModel.onEvent(event = RadarEvent.OnMapLoaded)

            assertEquals(
                expected = Triple(
                    first = false,
                    second = false,
                    third = true
                ),
                actual = Triple(
                    first = beforeAnything,
                    second = imageWithoutMap,
                    third = radarViewModel.state.value.isDockReady
                )
            )
        }

    @Test
    fun `the dock hides while another layer loads and shows when it is drawn`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            radarViewModel.onEvent(event = RadarEvent.OnMapLoaded)
            advanceUntilIdle()
            val seen = mutableListOf<Boolean>()

            listOf(RadarLayer.WIND, RadarLayer.POLLEN).forEach { layer: RadarLayer ->
                radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = layer))
                seen += radarViewModel.state.value.isDockReady
                advanceUntilIdle()
                seen += radarViewModel.state.value.isDockReady
            }

            assertEquals(
                expected = listOf(false, true, false, true),
                actual = seen
            )
        }

    @Test
    fun `a radar or image that cannot be loaded shows no dock`() =
        testScope.runTest {
            val failures: List<() -> Unit> = listOf(
                { getRadarTask.result = Failure(errorCode = ErrorCode.RADAR_NOT_AVAILABLE).asLeft() },
                {
                    getRadarTask.result = radar.asRight()
                    radarImageLoader.failingUrls = setOf("https://smhi.se/2.png")
                },
                {
                    getRadarTask.result = Radar(
                        images = emptyList(),
                        fetchedAt = FETCHED_AT
                    ).asRight()
                }
            )

            val isDockReady: List<Boolean> = failures.map { fail: () -> Unit ->
                fail()
                val radarViewModel: RadarViewModel = viewModel()
                radarViewModel.onEvent(event = RadarEvent.OnMapLoaded)
                advanceUntilIdle()
                radarViewModel.state.value.isDockReady
            }

            assertEquals(expected = List(size = failures.size) { false }, actual = isDockReady)
        }

    @Test
    fun `the radar stations start hidden and toggle on and off`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            val shown: List<Boolean> = listOf(radarViewModel.state.value.areRadarStationsShown) +
                List(size = 2) {
                    radarViewModel.onEvent(event = RadarEvent.OnRadarStationsToggled)
                    radarViewModel.state.value.areRadarStationsShown
                }

            assertEquals(expected = listOf(false, true, false), actual = shown)
        }

    @Test
    fun `turning pollen off hides the pollen layer and leaves it when it is shown`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.POLLEN))
            advanceUntilIdle()
            val layersWithPollen: List<RadarLayer> = radarViewModel.state.value.layers

            observePollenPreferencesTask.pollenPreferences.value = defaultPollenPreferences.copy(isPollenEnabled = false).asRight()
            advanceUntilIdle()

            assertEquals(
                expected = listOf(
                    RadarLayer.PRECIPITATION,
                    RadarLayer.WIND,
                    RadarLayer.POLLEN
                ),
                actual = layersWithPollen
            )
            assertEquals(
                expected = Triple(
                    listOf(
                        RadarLayer.PRECIPITATION,
                        RadarLayer.WIND
                    ),
                    RadarLayer.PRECIPITATION,
                    0
                ),
                actual = Triple(
                    radarViewModel.state.value.layers,
                    radarViewModel.state.value.layer,
                    radarViewModel.state.value.layerIndex
                )
            )
        }

    @Test
    fun `allergens are listed first and the map opens on the strongest of them that is flying`() =
        testScope.runTest {
            observePollenPreferencesTask.pollenPreferences.value = defaultPollenPreferences.copy(allergenPollenIds = setOf("birch")).asRight()
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.POLLEN))
            advanceUntilIdle()
            val opened: Pair<List<PollenType>, String?> = radarViewModel.state.value.pollenTypes to radarViewModel.state.value.selectedPollenId
            observePollenPreferencesTask.pollenPreferences.value = defaultPollenPreferences.copy(allergenPollenIds = setOf("grass")).asRight()
            advanceUntilIdle()

            assertEquals(
                expected = listOf<Pair<List<PollenType>, String?>>(
                    listOf(birch, grass) to "birch",
                    listOf(grass, birch) to "birch"
                ),
                actual = listOf(
                    opened,
                    radarViewModel.state.value.pollenTypes to radarViewModel.state.value.selectedPollenId
                )
            )
        }

    @Test
    fun `off season hides the pollen layer and tells once that pollen hibernates`() =
        testScope.runTest {
            observePollenInSeasonTask.isInSeason.value = false.asRight()
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            val isVisibleOnArrival: Boolean = radarViewModel.state.value.isPollenHibernationNoticeVisible

            radarViewModel.onEvent(event = RadarEvent.OnPollenHibernationNoticeDismissed)
            advanceUntilIdle()
            observePollenPreferencesTask.pollenPreferences.value = defaultPollenPreferences.copy(allergenPollenIds = setOf("birch")).asRight()
            advanceUntilIdle()
            val isVisibleBeforeStored: Boolean = radarViewModel.state.value.isPollenHibernationNoticeVisible
            observePollenPreferencesTask.pollenPreferences.value = defaultPollenPreferences.copy(isHibernationNoticeShown = true).asRight()
            advanceUntilIdle()

            assertEquals(
                expected = listOf(true, false, false),
                actual = listOf(
                    isVisibleOnArrival,
                    isVisibleBeforeStored,
                    radarViewModel.state.value.isPollenHibernationNoticeVisible
                )
            )
            assertEquals<List<Any>>(
                expected = listOf(
                    listOf(true),
                    listOf(RadarLayer.PRECIPITATION, RadarLayer.WIND)
                ),
                actual = listOf(
                    setPollenHibernationNoticeShownTask.shownValues,
                    radarViewModel.state.value.layers
                )
            )
        }

    @Test
    fun `a season that ends while the radar is open hides pollen and a readied notice shows again after dismissal`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.POLLEN))
            advanceUntilIdle()

            observePollenInSeasonTask.isInSeason.value = false.asRight()
            advanceUntilIdle()
            val afterEnd: Triple<RadarLayer, List<RadarLayer>, Boolean> = Triple(
                radarViewModel.state.value.layer,
                radarViewModel.state.value.layers,
                radarViewModel.state.value.isPollenHibernationNoticeVisible
            )
            radarViewModel.onEvent(event = RadarEvent.OnPollenHibernationNoticeDismissed)
            observePollenPreferencesTask.pollenPreferences.value = defaultPollenPreferences.copy(isHibernationNoticeShown = true).asRight()
            advanceUntilIdle()
            val afterDismissal: Boolean = radarViewModel.state.value.isPollenHibernationNoticeVisible
            observePollenPreferencesTask.pollenPreferences.value = defaultPollenPreferences.asRight()
            advanceUntilIdle()

            assertEquals(
                expected = Triple(
                    RadarLayer.PRECIPITATION,
                    listOf(RadarLayer.PRECIPITATION, RadarLayer.WIND),
                    true
                ),
                actual = afterEnd
            )
            assertEquals(
                expected = false to true,
                actual = afterDismissal to radarViewModel.state.value.isPollenHibernationNoticeVisible
            )
        }

    @Test
    fun `an off season says nothing when pollen is off or the notice was already shown`() =
        testScope.runTest {
            observePollenInSeasonTask.isInSeason.value = false.asRight()
            val noticesShown: List<Boolean> = listOf(
                defaultPollenPreferences.copy(isPollenEnabled = false),
                defaultPollenPreferences.copy(isHibernationNoticeShown = true)
            ).map { pollenPreferences: PollenPreferences ->
                observePollenPreferencesTask.pollenPreferences.value = pollenPreferences.asRight()
                val radarViewModel: RadarViewModel = viewModel()
                advanceUntilIdle()
                radarViewModel.state.value.isPollenHibernationNoticeVisible
            }

            assertEquals(
                expected = listOf(false, false),
                actual = noticesShown
            )
            assertEquals(
                expected = emptyList(),
                actual = setPollenHibernationNoticeShownTask.shownValues
            )
        }

    @Test
    fun `a season that is back readies the notice for the next off season`() =
        testScope.runTest {
            observePollenPreferencesTask.pollenPreferences.value = defaultPollenPreferences.copy(isHibernationNoticeShown = true).asRight()
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                expected = listOf(false) to false,
                actual = setPollenHibernationNoticeShownTask.shownValues to radarViewModel.state.value.isPollenHibernationNoticeVisible
            )
            assertEquals<List<RadarLayer>>(
                expected = RadarLayer.entries,
                actual = radarViewModel.state.value.layers
            )
        }

    @Test
    fun `a season that cannot be told keeps the pollen layer hidden and says nothing`() =
        testScope.runTest {
            observePollenInSeasonTask.isInSeason.value = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                expected = listOf(RadarLayer.PRECIPITATION, RadarLayer.WIND) to false,
                actual = radarViewModel.state.value.layers to radarViewModel.state.value.isPollenHibernationNoticeVisible
            )
        }

    @Test
    fun `pollen preferences that cannot be read keep every layer`() =
        testScope.runTest {
            observePollenPreferencesTask.pollenPreferences.value = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft()
            val radarViewModel: RadarViewModel = viewModel()
            advanceUntilIdle()

            assertEquals<List<RadarLayer>>(
                expected = RadarLayer.entries,
                actual = radarViewModel.state.value.layers
            )
        }
}