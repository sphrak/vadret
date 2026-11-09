package se.vadretapp.vadret.forecastwarning

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.extension.onResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.Aurora
import se.vadretapp.vadret.aurora.model.AuroraHour
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastwarning.extension.toWarningItem
import se.vadretapp.vadret.forecastwarning.extension.withAuroraHours
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.extension.launchReplacing
import se.vadretapp.vadret.ui.format.TextLocale
import se.vadretapp.vadret.ui.format.currentLanguageTag
import se.vadretapp.vadret.warning.usecase.GetAuroraHoursTask
import se.vadretapp.vadret.warning.usecase.GetWarningsTask
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Clock

private const val FIRST_INDEX = 0
private const val STORY_DETAIL_COUNT = 1

@Immutable
class WarningStoryViewModel internal constructor(
    private val getWarningsTask: GetWarningsTask,
    private val getAuroraHoursTask: GetAuroraHoursTask,
    private val markWarningSeenTask: MarkWarningSeenTask,
    private val clock: Clock,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<WarningStoryState>
        field = MutableStateFlow(value = WarningStoryState())

    private val stepDispatcher: CoroutineDispatcher = coroutineDispatcher.limitedParallelism(parallelism = 1)
    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)
    private val textLocale: AtomicReference<TextLocale> = AtomicReference(
        value = TextLocale(
            languageTag = currentLanguageTag(),
            hourFormat = HourFormat.TWENTY_FOUR_HOUR
        )
    )
    private val textLocaleJob: AtomicReference<Job?> = AtomicReference(value = null)
    private val stories: AtomicReference<List<AggregatedFeed>> = AtomicReference(value = emptyList())
    private val auroraHoursByIdentifier: AtomicReference<Map<String, List<AuroraHour>>> = AtomicReference(value = emptyMap())

    internal fun onEvent(event: WarningStoryEvent) {
        when (event) {
            is WarningStoryEvent.OnStart -> onStart(warningIdentifiers = event.warningIdentifiers)
            is WarningStoryEvent.OnStoryShown -> onStoryShown(index = event.index)
            WarningStoryEvent.OnNext -> step { state: WarningStoryState ->
                if (state.currentIndex < state.warnings.lastIndex) {
                    state.copy(currentIndex = state.currentIndex + 1)
                } else {
                    state.copy(isFinished = true)
                }
            }
            WarningStoryEvent.OnPrevious -> step { state: WarningStoryState ->
                state.copy(currentIndex = (state.currentIndex - 1).coerceAtLeast(minimumValue = FIRST_INDEX))
            }
            is WarningStoryEvent.OnLocaleChanged -> onLocaleChanged(
                languageTag = event.languageTag,
                hourFormat = event.hourFormat
            )
        }
    }

    private fun onStart(warningIdentifiers: List<String>) {
        val isFirstStart: Boolean = isStarted.compareAndSet(
            expectedValue = false,
            newValue = true
        )
        if (!isFirstStart) {
            return
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            getWarningsTask()
                .onResult(
                    onError = { _: Failure -> finish() },
                    onSuccess = { aggregatedFeeds: List<AggregatedFeed> ->
                        val aggregatedFeedsByIdentifier: Map<String, AggregatedFeed> = aggregatedFeeds.associateBy { aggregatedFeed: AggregatedFeed -> aggregatedFeed.identifier }
                        stories.store(newValue = warningIdentifiers.mapNotNull { warningIdentifier: String -> aggregatedFeedsByIdentifier[warningIdentifier] })
                        displayStories()
                        loadAuroraHours()
                    }
                )
        }
    }

    private suspend fun loadAuroraHours() {
        val loadedAuroraHours: Map<String, List<AuroraHour>> = stories
            .load()
            .associate { aggregatedFeed: AggregatedFeed -> aggregatedFeed.identifier to auroraHoursOf(aggregatedFeed = aggregatedFeed) }
            .filterValues { auroraHours: List<AuroraHour> -> auroraHours.isNotEmpty() }
        if (loadedAuroraHours.isNotEmpty()) {
            auroraHoursByIdentifier.store(newValue = loadedAuroraHours)
            displayStories()
        }
    }

    private suspend fun auroraHoursOf(aggregatedFeed: AggregatedFeed): List<AuroraHour> {
        val aurora: Aurora = aggregatedFeed.aurora ?: return emptyList()
        return getAuroraHoursTask(aurora = aurora).onResult(
            onError = { failure: Failure ->
                logger.debug { "The aurora story ${aggregatedFeed.identifier} is shown without its hours: ${failure.errorCode}" }
                emptyList()
            },
            onSuccess = { auroraHours: List<AuroraHour> -> auroraHours }
        )
    }

    private fun onStoryShown(index: Int) {
        val aggregatedFeed: AggregatedFeed = stories.load().getOrNull(index = index) ?: return
        viewModelScope.launch(context = coroutineDispatcher) {
            markWarningSeenTask(warningIdentifier = aggregatedFeed.identifier)
        }
    }

    private fun onLocaleChanged(
        languageTag: String,
        hourFormat: HourFormat
    ) {
        val shownTextLocale = TextLocale(
            languageTag = languageTag,
            hourFormat = hourFormat
        )
        if (textLocale.exchange(newValue = shownTextLocale) == shownTextLocale) {
            return
        }
        // The language and clock change together and the newest change is the one shown, however the redrawing interleaves.
        viewModelScope.launchReplacing(
            currentJob = textLocaleJob,
            context = coroutineDispatcher
        ) {
            if (stories.load().isNotEmpty()) {
                displayStories()
            }
        }
    }

    private fun step(transform: (WarningStoryState) -> WarningStoryState) {
        viewModelScope.launch(context = stepDispatcher) {
            state.update { state: WarningStoryState ->
                if (state.isLoading || state.isFinished) {
                    state
                } else {
                    transform(state)
                }
            }
        }
    }

    private fun displayStories() {
        val shownTextLocale: TextLocale = textLocale.load()
        val shownAuroraHours: Map<String, List<AuroraHour>> = auroraHoursByIdentifier.load()
        val warnings: ImmutableList<WarningItem> = stories
            .load()
            .map { aggregatedFeed: AggregatedFeed ->
                aggregatedFeed
                    .toWarningItem(
                        now = clock.now(),
                        timeZone = TimeZone.currentSystemDefault(),
                        languageTag = shownTextLocale.languageTag,
                        hourFormat = shownTextLocale.hourFormat
                    ).withAuroraHours(
                        auroraHours = shownAuroraHours[aggregatedFeed.identifier].orEmpty(),
                        now = clock.now(),
                        timeZone = TimeZone.currentSystemDefault(),
                        languageTag = shownTextLocale.languageTag,
                        hourFormat = shownTextLocale.hourFormat
                    ).withStoryDetails()
            }.toImmutableList()
        state.update { state: WarningStoryState ->
            state.copy(
                warnings = warnings,
                isLoading = false,
                isFinished = warnings.isEmpty()
            )
        }
    }

    private fun finish() {
        state.update { state: WarningStoryState ->
            state.copy(
                isLoading = false,
                isFinished = true
            )
        }
    }

    private fun WarningItem.withStoryDetails(): WarningItem = copy(details = details.take(n = STORY_DETAIL_COUNT).toImmutableList())
}

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.forecastwarning")