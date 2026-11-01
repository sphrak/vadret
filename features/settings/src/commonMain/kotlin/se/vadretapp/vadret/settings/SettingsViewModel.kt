package se.vadretapp.vadret.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.model.AppInfo
import se.vadretapp.vadret.settings.extension.toLibraryGroups
import se.vadretapp.vadret.settings.extension.toLocationRows
import se.vadretapp.vadret.theme.model.Theme
import kotlin.concurrent.atomics.AtomicBoolean

class SettingsViewModel internal constructor(
    private val getSettingsTask: GetSettingsTask,
    private val setLocationModeTask: SetLocationModeTask,
    private val setThemeTask: SetThemeTask,
    private val setLanguageTask: SetLanguageTask,
    private val setHourlyForecastTask: SetHourlyForecastTask,
    private val setExpandOnceTask: SetExpandOnceTask,
    private val getLibrariesTask: GetLibrariesTask,
    private val getChangelogTask: GetChangelogTask,
    appInfo: AppInfo,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<SettingsState>
        field = MutableStateFlow(
            value = SettingsState(
                pages = settingsPages(isDeveloperBuild = appInfo.isDeveloperBuild),
                aboutRows = aboutRows(appVersion = appInfo.versionName)
            )
        )

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)

    internal fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.OnStart -> onStart()
            is SettingsEvent.OnLocationModeToggled -> onLocationModeToggled(
                isAutomaticLocation = event.isAutomaticLocation
            )
            is SettingsEvent.OnHourlyForecastToggled -> onHourlyForecastToggled(
                isHourlyForecastEnabled = event.isHourlyForecastEnabled
            )
            is SettingsEvent.OnExpandOnceToggled -> onExpandOnceToggled(
                isExpandOnceEnabled = event.isExpandOnceEnabled
            )
            SettingsEvent.OnThemeRowClicked -> state.update { state: SettingsState ->
                state.copy(isThemeDialogVisible = true)
            }
            SettingsEvent.OnThemeDialogDismissed -> state.update { state: SettingsState ->
                state.copy(isThemeDialogVisible = false)
            }
            is SettingsEvent.OnThemeSelected -> onThemeSelected(theme = event.theme)
            SettingsEvent.OnLanguageRowClicked -> state.update { state: SettingsState ->
                state.copy(isLanguageDialogVisible = true)
            }
            SettingsEvent.OnLanguageDialogDismissed -> state.update { state: SettingsState ->
                state.copy(isLanguageDialogVisible = false)
            }
            is SettingsEvent.OnLanguageSelected -> onLanguageSelected(language = event.language)
            is SettingsEvent.OnLibraryClicked -> state.update { state: SettingsState ->
                state.copy(selectedLibrary = event.library)
            }
            SettingsEvent.OnLibraryDismissed -> state.update { state: SettingsState ->
                state.copy(selectedLibrary = null)
            }
            SettingsEvent.OnChangelogClicked -> state.update { state: SettingsState ->
                state.copy(isChangelogVisible = state.changelog != null)
            }
            SettingsEvent.OnChangelogDismissed -> state.update { state: SettingsState ->
                state.copy(isChangelogVisible = false)
            }
        }
    }

    private fun onStart() {
        if (isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            viewModelScope.launch(context = coroutineDispatcher) {
                loadSettings()
                loadLibraries()
                loadChangelog()
            }
        }
    }

    private suspend fun loadSettings() {
        getSettingsTask().onResult(
            onError = { _: Failure -> },
            onSuccess = { settings: Settings ->
                state.update { state: SettingsState ->
                    state.copy(
                        settings = settings,
                        locationRows = settings.toLocationRows()
                    )
                }
            }
        )
    }

    private fun loadLibraries() {
        getLibrariesTask().onResult(
            onError = { },
            onSuccess = { libraries: List<Library> ->
                state.update { state: SettingsState ->
                    state.copy(libraryGroups = libraries.toLibraryGroups())
                }
            }
        )
    }

    private suspend fun loadChangelog() {
        getChangelogTask().onResult(
            onError = { _: Failure -> },
            onSuccess = { changelog: Changelog ->
                state.update { state: SettingsState ->
                    state.copy(changelog = changelog)
                }
            }
        )
    }

    private fun onLocationModeToggled(isAutomaticLocation: Boolean) {
        viewModelScope.launch(context = coroutineDispatcher) {
            setLocationModeTask(isAutomaticLocation = isAutomaticLocation).onResult(
                onError = { _: Failure -> },
                onSuccess = { loadSettings() }
            )
        }
    }

    private fun onHourlyForecastToggled(isHourlyForecastEnabled: Boolean) {
        viewModelScope.launch(context = coroutineDispatcher) {
            setHourlyForecastTask(isHourlyForecastEnabled = isHourlyForecastEnabled).onResult(
                onError = { _: Failure -> },
                onSuccess = { loadSettings() }
            )
        }
    }

    private fun onExpandOnceToggled(isExpandOnceEnabled: Boolean) {
        viewModelScope.launch(context = coroutineDispatcher) {
            setExpandOnceTask(isExpandOnceEnabled = isExpandOnceEnabled).onResult(
                onError = { _: Failure -> },
                onSuccess = { loadSettings() }
            )
        }
    }

    private fun onLanguageSelected(language: AppLanguage) {
        state.update { state: SettingsState ->
            state.copy(isLanguageDialogVisible = false)
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            setLanguageTask(language = language).onResult(
                onError = { _: Failure -> },
                onSuccess = { loadSettings() }
            )
        }
    }

    private fun onThemeSelected(theme: Theme) {
        state.update { state: SettingsState ->
            state.copy(isThemeDialogVisible = false)
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            setThemeTask(theme = theme).onResult(
                onError = { _: Failure -> },
                onSuccess = { loadSettings() }
            )
        }
    }
}