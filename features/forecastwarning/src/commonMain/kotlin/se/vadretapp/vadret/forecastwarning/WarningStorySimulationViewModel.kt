package se.vadretapp.vadret.forecastwarning

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.extension.onResult
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastwarning.extension.toWarningItem
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.extension.launchReplacing
import se.vadretapp.vadret.warning.usecase.GetWarningsTask
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Clock

// Lists the simulated warnings the way the warnings tab shows real ones, so each can be opened as its story.
@Immutable
class WarningStorySimulationViewModel internal constructor(
    private val getWarningsTask: GetWarningsTask,
    private val clock: Clock,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<WarningStorySimulationState>
        field = MutableStateFlow(value = WarningStorySimulationState())

    private val listJob: AtomicReference<Job?> = AtomicReference(value = null)

    internal fun onEvent(event: WarningStorySimulationEvent) {
        when (event) {
            is WarningStorySimulationEvent.OnLocaleChanged -> showWarnings(
                languageTag = event.languageTag,
                hourFormat = event.hourFormat
            )
        }
    }

    private fun showWarnings(
        languageTag: String,
        hourFormat: HourFormat
    ) {
        viewModelScope.launchReplacing(
            currentJob = listJob,
            context = coroutineDispatcher
        ) {
            getWarningsTask().onResult(
                onError = { failure: Failure -> logger.warn { "The simulated warnings could not be listed: ${failure.errorCode}" } },
                onSuccess = { aggregatedFeeds: List<AggregatedFeed> ->
                    state.update { state: WarningStorySimulationState ->
                        state.copy(
                            warnings = aggregatedFeeds.map { aggregatedFeed: AggregatedFeed ->
                                aggregatedFeed.toWarningItem(
                                    now = clock.now(),
                                    timeZone = TimeZone.currentSystemDefault(),
                                    languageTag = languageTag,
                                    hourFormat = hourFormat
                                )
                            }.toImmutableList()
                        )
                    }
                }
            )
        }
    }
}

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.forecastwarning")