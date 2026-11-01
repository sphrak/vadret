package se.vadretapp.vadret.radar

import androidx.compose.ui.graphics.ImageBitmap
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.network_failure
import se.vadretapp.vadret.core.resources.no_radar_available
import se.vadretapp.vadret.core.resources.no_wind_available
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.model.RadarFrame
import se.vadretapp.vadret.radar.model.RadarImage
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.model.WindTimeFrame
import se.vadretapp.vadret.radar.precipitation.PrecipitationMotion
import se.vadretapp.vadret.radar.precipitation.PrecipitationOverlay
import se.vadretapp.vadret.radar.precipitation.PrecipitationRenderer
import se.vadretapp.vadret.radar.precipitation.RadarEchoes
import se.vadretapp.vadret.radar.wind.WindField
import se.vadretapp.vadret.radar.wind.WindFieldRenderer
import se.vadretapp.vadret.radar.wind.WindFieldValues
import se.vadretapp.vadret.radar.wind.WindPalette
import se.vadretapp.vadret.testing.FixedClock
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
import kotlin.time.Instant

private class FakeGetRadarTask(
    var result: Either<Failure, Radar>
) : GetRadarTask {

    val requestedImageUrls = mutableListOf<String?>()

    override suspend fun invoke(imageUrl: String?): Either<Failure, Radar> {
        requestedImageUrls.add(imageUrl)
        return result
    }
}

private class FakeRadarImageLoader : RadarImageLoader {

    val loadedUrls = mutableListOf<String>()
    val radarEchoes = mutableMapOf<String, RadarEchoes>()
    var failingUrls: Set<String> = emptySet()

    override suspend fun load(imageUrl: String): Either<Failure, RadarEchoes> {
        loadedUrls.add(imageUrl)
        return if (imageUrl in failingUrls) {
            Failure(errorCode = ErrorCode.NETWORK_FAILURE).asLeft()
        } else {
            radarEchoes.getOrPut(imageUrl) { fakeRadarEchoes() }.asRight()
        }
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

private class FakeGetWindForecastTask(
    var result: Either<Failure, WindForecast>
) : GetWindForecastTask {

    var requestCount: Int = 0

    override suspend fun invoke(): Either<Failure, WindForecast> {
        requestCount++
        return result
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
    }
)

private val radar = Radar(
    images = (0..2).map { minute: Int ->
        RadarImage(
            imageUrl = "https://smhi.se/$minute.png",
            validAt = Instant.parse(input = "2026-10-24T12:0$minute:00Z")
        )
    }
)

@OptIn(ExperimentalCoroutinesApi::class)
class RadarViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val getRadarTask = FakeGetRadarTask(result = radar.asRight())
    private val getWindForecastTask = FakeGetWindForecastTask(result = windForecast.asRight())
    private val getDeviceLocationTask = FakeGetDeviceLocationTask(result = Location(latitude = 59.3, longitude = 18.0).asRight())
    private val radarImageLoader = FakeRadarImageLoader()
    private val windFieldRenderer = FakeWindFieldRenderer()

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
            getRadarTask = getRadarTask,
            getWindForecastTask = getWindForecastTask,
            getDeviceLocationTask = getDeviceLocationTask,
            radarImageLoader = radarImageLoader,
            precipitationRenderer = FakePrecipitationRenderer(),
            windFieldRenderer = windFieldRenderer,
            clock = FixedClock(now = Instant.parse(input = "2026-10-24T12:45:00Z")),
            coroutineDispatcher = testDispatcher
        )
        if (isStarted) {
            radarViewModel.onEvent(event = RadarEvent.OnStart(initialImageUrl = initialImageUrl))
        }
        return radarViewModel
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
    fun `playing loads the frames a few steps ahead`() =
        testScope.runTest {
            getRadarTask.result = Radar(
                images = (0..5).map { minute: Int ->
                    RadarImage(
                        imageUrl = "https://smhi.se/$minute.png",
                        validAt = Instant.parse(input = "2026-10-24T12:0$minute:00Z")
                    )
                }
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
            radarViewModel.onEvent(event = RadarEvent.OnLocaleChanged(languageTag = "en"))
            radarViewModel.onEvent(event = RadarEvent.OnStart(initialImageUrl = null))
            advanceUntilIdle()
            radarViewModel.onEvent(event = RadarEvent.OnIndexChanged(index = 1))
            radarViewModel.onEvent(event = RadarEvent.OnLayerSelected(layer = RadarLayer.WIND))
            advanceUntilIdle()
            val english: RadarState = radarViewModel.state.value

            radarViewModel.onEvent(event = RadarEvent.OnLocaleChanged(languageTag = "sv-SE"))
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
            radarViewModel.onEvent(event = RadarEvent.OnLocaleChanged(languageTag = "en"))
            radarViewModel.onEvent(event = RadarEvent.OnStart(initialImageUrl = null))
            advanceUntilIdle()
            val before: RadarState = radarViewModel.state.value

            radarViewModel.onEvent(event = RadarEvent.OnLocaleChanged(languageTag = "en"))
            advanceUntilIdle()

            assertSame(expected = before, actual = radarViewModel.state.value)
        }

    @Test
    fun `a language chosen before anything loaded is used once it loads`() =
        testScope.runTest {
            val radarViewModel: RadarViewModel = viewModel(isStarted = false)
            radarViewModel.onEvent(event = RadarEvent.OnLocaleChanged(languageTag = "sv-SE"))
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
                { getRadarTask.result = Radar(images = emptyList()).asRight() }
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
}