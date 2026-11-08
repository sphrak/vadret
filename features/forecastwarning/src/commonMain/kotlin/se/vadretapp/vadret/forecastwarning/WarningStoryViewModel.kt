package se.vadretapp.vadret.forecastwarning

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.extension.onResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastwarning.extension.toWarningItem
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.format.currentLanguageTag
import se.vadretapp.vadret.warning.usecase.GetWarningsTask
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Clock

private const val FIRST_INDEX = 0
private const val STORY_DETAIL_COUNT = 1

@Immutable
class WarningStoryViewModel internal constructor(
    private val getWarningsTask: GetWarningsTask,
    private val markWarningSeenTask: MarkWarningSeenTask,
    private val clock: Clock,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<WarningStoryState>
        field = MutableStateFlow(value = WarningStoryState())

    private val stepDispatcher: CoroutineDispatcher = coroutineDispatcher.limitedParallelism(parallelism = 1)
    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)
    private val languageTag: AtomicReference<String> = AtomicReference(value = currentLanguageTag())
    private val hourFormat: AtomicReference<HourFormat> = AtomicReference(value = HourFormat.TWENTY_FOUR_HOUR)
    private val stories: AtomicReference<List<AggregatedFeed>> = AtomicReference(value = emptyList())

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
                    }
                )
        }
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
        val isLanguageSame: Boolean = this.languageTag.exchange(newValue = languageTag) == languageTag
        val isHourFormatSame: Boolean = this.hourFormat.exchange(newValue = hourFormat) == hourFormat
        if (isLanguageSame && isHourFormatSame) {
            return
        }
        viewModelScope.launch(context = coroutineDispatcher) {
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
        val warnings: ImmutableList<WarningItem> = stories
            .load()
            .map { aggregatedFeed: AggregatedFeed ->
                aggregatedFeed
                    .toWarningItem(
                        now = clock.now(),
                        timeZone = TimeZone.currentSystemDefault(),
                        languageTag = languageTag.load(),
                        hourFormat = hourFormat.load()
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