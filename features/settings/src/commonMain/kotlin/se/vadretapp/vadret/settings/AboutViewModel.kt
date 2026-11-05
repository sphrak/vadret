package se.vadretapp.vadret.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.di.model.AppInfo
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.developer_already_unlocked
import se.vadretapp.vadret.ui.resources.developer_unlocked
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.incrementAndFetch

private const val VERSION_TAPS_TO_UNLOCK = 7

class AboutViewModel internal constructor(
    private val getChangelogTask: GetChangelogTask,
    private val observeDeveloperModeTask: ObserveDeveloperModeTask,
    private val unlockDeveloperModeTask: UnlockDeveloperModeTask,
    appInfo: AppInfo,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<AboutState>
        field = MutableStateFlow(value = AboutState(aboutRows = aboutRows(appVersion = appInfo.versionName)))

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)
    private val isDeveloperMode: AtomicReference<Boolean?> = AtomicReference(value = null)
    private val versionTaps: AtomicInt = AtomicInt(value = 0)

    internal fun onEvent(event: AboutEvent) {
        when (event) {
            AboutEvent.OnStart -> onStart()
            AboutEvent.OnVersionClicked -> onVersionClicked()
            AboutEvent.OnChangelogClicked -> state.update { state: AboutState -> state.copy(isChangelogVisible = state.changelog != null) }
            AboutEvent.OnChangelogDismissed -> state.update { state: AboutState -> state.copy(isChangelogVisible = false) }
            AboutEvent.OnToastShown -> state.update { state: AboutState -> state.copy(toastResource = null) }
        }
    }

    private fun onStart() {
        if (isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            viewModelScope.launch(context = coroutineDispatcher) { loadChangelog() }
            viewModelScope.launch(context = coroutineDispatcher) { observeDeveloperMode() }
        }
    }

    private suspend fun loadChangelog() {
        getChangelogTask().onResult(
            onError = { _: Failure -> },
            onSuccess = { changelog: Changelog -> state.update { state: AboutState -> state.copy(changelog = changelog) } }
        )
    }

    private suspend fun observeDeveloperMode() {
        observeDeveloperModeTask().collect { result: Either<Failure, Boolean> ->
            result.onResult(
                onError = { _: Failure -> },
                onSuccess = { isUnlocked: Boolean -> isDeveloperMode.store(newValue = isUnlocked) }
            )
        }
    }

    // Like the build number on Android, the seventh tap on the version unlocks the developer settings. Taps before
    // the stored choice is read are not counted, so an unlocked app is never unlocked again.
    private fun onVersionClicked() {
        when (isDeveloperMode.load()) {
            null -> {}
            true -> state.update { state: AboutState -> state.copy(toastResource = Res.string.developer_already_unlocked) }
            false -> countVersionTap()
        }
    }

    private fun countVersionTap() {
        if (versionTaps.incrementAndFetch() == VERSION_TAPS_TO_UNLOCK) {
            viewModelScope.launch(context = coroutineDispatcher) {
                unlockDeveloperModeTask().onResult(
                    onError = { _: Failure -> versionTaps.store(newValue = 0) },
                    onSuccess = {
                        state.update { state: AboutState -> state.copy(toastResource = Res.string.developer_unlocked) }
                    }
                )
            }
        }
    }
}