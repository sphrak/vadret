package se.vadretapp.vadret.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.onResult
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.ObserveFollowablePlacesTask
import se.vadretapp.vadret.followedplace.SetPlaceFollowedTask
import se.vadretapp.vadret.followedplace.model.FollowableFavorite
import se.vadretapp.vadret.followedplace.model.FollowablePlaces
import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.model.AppInfo
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.settings.extension.toFollowedPlaceRow
import se.vadretapp.vadret.settings.extension.toLibraryGroups
import se.vadretapp.vadret.settings.extension.toLocationRows
import se.vadretapp.vadret.settings.extension.toPollenAllergenRows
import se.vadretapp.vadret.settings.model.FollowedPlaceRow
import se.vadretapp.vadret.theme.model.Theme
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference

class SettingsViewModel internal constructor(
    private val getSettingsTask: GetSettingsTask,
    private val setLocationModeTask: SetLocationModeTask,
    private val setThemeTask: SetThemeTask,
    private val setLanguageTask: SetLanguageTask,
    private val setHourlyForecastTask: SetHourlyForecastTask,
    private val setExpandOnceTask: SetExpandOnceTask,
    private val getLibrariesTask: GetLibrariesTask,
    private val getChangelogTask: GetChangelogTask,
    private val setPollenEnabledTask: SetPollenEnabledTask,
    private val setPollenAllergenTask: SetPollenAllergenTask,
    private val getPollenTypesTask: GetPollenTypesTask,
    private val setEndOfSeasonSimulatedTask: SetEndOfSeasonSimulatedTask,
    private val observeFollowablePlacesTask: ObserveFollowablePlacesTask,
    private val setPlaceFollowedTask: SetPlaceFollowedTask,
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
    private val pollenTypes: AtomicReference<List<PollenType>> = AtomicReference(value = emptyList())

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
            is SettingsEvent.OnPollenToggled -> onPollenToggled(isPollenEnabled = event.isPollenEnabled)
            SettingsEvent.OnPollenAllergensRowClicked -> onPollenAllergensRowClicked()
            SettingsEvent.OnPollenAllergensDialogDismissed -> state.update { state: SettingsState ->
                state.copy(isPollenAllergenDialogVisible = false)
            }
            is SettingsEvent.OnPollenAllergenToggled -> onPollenAllergenToggled(
                pollenId = event.pollenId,
                isAllergen = event.isAllergen
            )
            is SettingsEvent.OnEndOfSeasonSimulationToggled -> onEndOfSeasonSimulationToggled(
                isEndOfSeasonSimulated = event.isEndOfSeasonSimulated
            )
            is SettingsEvent.OnPlaceFollowToggled -> onPlaceFollowToggled(
                favoriteId = event.favoriteId,
                isFollowed = event.isFollowed
            )
            SettingsEvent.OnFollowedPlaceLimitDialogDismissed -> state.update { state: SettingsState ->
                state.copy(isFollowedPlaceLimitDialogVisible = false)
            }
            SettingsEvent.OnFollowedPlaceAbroadDialogDismissed -> state.update { state: SettingsState ->
                state.copy(isFollowedPlaceAbroadDialogVisible = false)
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
            viewModelScope.launch(context = coroutineDispatcher) { observeFollowablePlaces() }
        }
    }

    // The chosen place is always followed, so only the other favorites have a switch.
    private suspend fun observeFollowablePlaces() {
        observeFollowablePlacesTask().collect { result: Either<Failure, FollowablePlaces> ->
            result.onResult(
                onError = { _: Failure -> },
                onSuccess = { followablePlaces: FollowablePlaces ->
                    state.update { state: SettingsState ->
                        state.copy(
                            chosenPlaceName = followablePlaces.chosenPlace?.name,
                            followedPlaceRows = followablePlaces.favorites
                                .filterNot { followableFavorite: FollowableFavorite -> followableFavorite.isChosen }
                                .map { followableFavorite: FollowableFavorite -> followableFavorite.toFollowedPlaceRow() }
                                .toImmutableList()
                        )
                    }
                }
            )
        }
    }

    // A favorite abroad has no warnings to follow, so it is explained instead of followed.
    private fun onPlaceFollowToggled(
        favoriteId: String,
        isFollowed: Boolean
    ) {
        val followedPlaceRow: FollowedPlaceRow = state.value.followedPlaceRows.firstOrNull { row: FollowedPlaceRow -> row.favoriteId == favoriteId } ?: return
        if (isFollowed && !followedPlaceRow.isInSweden) {
            state.update { state: SettingsState -> state.copy(isFollowedPlaceAbroadDialogVisible = true) }
            return
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            setPlaceFollowedTask(
                favoriteId = favoriteId,
                isFollowed = isFollowed
            ).onResult(
                onError = { failure: Failure ->
                    if (failure.errorCode == ErrorCode.FOLLOWED_PLACE_LIMIT) {
                        state.update { state: SettingsState -> state.copy(isFollowedPlaceLimitDialogVisible = true) }
                    }
                },
                onSuccess = {}
            )
        }
    }

    private suspend fun loadSettings() {
        getSettingsTask().onResult(
            onError = { _: Failure -> },
            onSuccess = { settings: Settings ->
                state.update { state: SettingsState ->
                    state.copy(
                        settings = settings,
                        locationRows = settings.toLocationRows(),
                        pollenAllergenCount = settings.allergenPollenIds.size,
                        pollenAllergenRows = pollenTypes.load().toPollenAllergenRows(allergenPollenIds = settings.allergenPollenIds)
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

    private fun onPollenToggled(isPollenEnabled: Boolean) {
        viewModelScope.launch(context = coroutineDispatcher) {
            setPollenEnabledTask(isPollenEnabled = isPollenEnabled).onResult(
                onError = { _: Failure -> },
                onSuccess = {
                    loadSettings()
                }
            )
        }
    }

    private fun onPollenAllergensRowClicked() {
        state.update { state: SettingsState ->
            state.copy(isPollenAllergenDialogVisible = true)
        }
        if (pollenTypes.load().isEmpty()) {
            viewModelScope.launch(context = coroutineDispatcher) {
                loadPollenTypes()
            }
        }
    }

    private suspend fun loadPollenTypes() {
        getPollenTypesTask().onResult(
            onError = { _: Failure ->
                state.update { state: SettingsState ->
                    state.copy(arePollenTypesUnavailable = true)
                }
            },
            onSuccess = { loadedPollenTypes: List<PollenType> ->
                pollenTypes.store(newValue = loadedPollenTypes)
                state.update { state: SettingsState ->
                    state.copy(
                        pollenAllergenRows = loadedPollenTypes.toPollenAllergenRows(allergenPollenIds = state.settings?.allergenPollenIds.orEmpty()),
                        arePollenTypesUnavailable = false
                    )
                }
            }
        )
    }

    private fun onPollenAllergenToggled(
        pollenId: String,
        isAllergen: Boolean
    ) {
        viewModelScope.launch(context = coroutineDispatcher) {
            setPollenAllergenTask(
                pollenId = pollenId,
                isAllergen = isAllergen
            ).onResult(
                onError = { _: Failure -> },
                onSuccess = { loadSettings() }
            )
        }
    }

    private fun onEndOfSeasonSimulationToggled(isEndOfSeasonSimulated: Boolean) {
        viewModelScope.launch(context = coroutineDispatcher) {
            setEndOfSeasonSimulatedTask(isEndOfSeasonSimulated = isEndOfSeasonSimulated).onResult(
                onError = { _: Failure -> },
                onSuccess = { loadSettings() }
            )
        }
    }
}