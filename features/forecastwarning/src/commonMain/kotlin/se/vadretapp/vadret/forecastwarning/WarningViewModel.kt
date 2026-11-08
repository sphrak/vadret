package se.vadretapp.vadret.forecastwarning

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.onResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastwarning.extension.followedPlaceNames
import se.vadretapp.vadret.forecastwarning.extension.toWarningItems
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.extension.launchReplacing
import se.vadretapp.vadret.ui.extension.messageResource
import se.vadretapp.vadret.ui.format.currentLanguageTag
import se.vadretapp.vadret.warning.model.FollowedPlaceWarnings
import se.vadretapp.vadret.warning.usecase.ObserveFollowedPlaceWarningsTask
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Clock

@Immutable
class WarningViewModel internal constructor(
    private val observeFollowedPlaceWarningsTask: ObserveFollowedPlaceWarningsTask,
    private val clock: Clock,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<WarningState>
        field = MutableStateFlow(value = WarningState())

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)
    private val languageTag: AtomicReference<String> = AtomicReference(value = currentLanguageTag())
    private val hourFormat: AtomicReference<HourFormat> = AtomicReference(value = HourFormat.TWENTY_FOUR_HOUR)
    private val warnings: AtomicReference<FollowedPlaceWarnings?> = AtomicReference(value = null)
    private val loadJob: AtomicReference<Job?> = AtomicReference(value = null)

    internal fun onEvent(event: WarningEvent) {
        when (event) {
            WarningEvent.OnStart -> onStart()
            WarningEvent.OnRefresh -> onRefresh()
            WarningEvent.OnErrorShown -> state.update { state: WarningState ->
                state.copy(errorMessageResource = null)
            }
            is WarningEvent.OnLocaleChanged -> onLocaleChanged(
                languageTag = event.languageTag,
                hourFormat = event.hourFormat
            )
        }
    }

    private fun onStart() {
        if (isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            load(isRefresh = false)
        }
    }

    private fun onRefresh() {
        state.update { state: WarningState ->
            state.copy(isRefreshing = true)
        }
        load(isRefresh = true)
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
            val loadedWarnings: FollowedPlaceWarnings = warnings.load() ?: return@launch
            state.update { state: WarningState ->
                state.copy(warnings = loadedWarnings.toItems())
            }
        }
    }

    private fun load(isRefresh: Boolean) {
        viewModelScope.launchReplacing(
            currentJob = loadJob,
            context = coroutineDispatcher
        ) {
            observeFollowedPlaceWarningsTask(isRefresh = isRefresh).collect { result: Either<Failure, FollowedPlaceWarnings> ->
                result.onResult(
                    onError = { failure: Failure -> displayError(failure = failure) },
                    onSuccess = { followedPlaceWarnings: FollowedPlaceWarnings ->
                        warnings.store(newValue = followedPlaceWarnings)
                        displayWarnings(
                            warningItems = followedPlaceWarnings.toItems(),
                            followedPlaceNames = followedPlaceWarnings.followedPlaceNames()
                        )
                    }
                )
            }
        }
    }

    private fun FollowedPlaceWarnings.toItems(): ImmutableList<WarningItem> =
        toWarningItems(
            now = clock.now(),
            timeZone = TimeZone.currentSystemDefault(),
            languageTag = languageTag.load(),
            hourFormat = hourFormat.load()
        )

    private fun displayWarnings(
        warningItems: ImmutableList<WarningItem>,
        followedPlaceNames: String?
    ) {
        state.update { state: WarningState ->
            state.copy(
                warnings = warningItems,
                followedPlaceNames = followedPlaceNames,
                isLoading = false,
                isRefreshing = false,
                isEmpty = warningItems.isEmpty()
            )
        }
    }

    private fun displayError(failure: Failure) {
        state.update { state: WarningState ->
            state.copy(
                isLoading = false,
                isRefreshing = false,
                errorMessageResource = failure.messageResource()
            )
        }
    }
}