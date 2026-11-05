package se.vadretapp.vadret.settings

internal sealed interface AboutEvent {
    data object OnStart : AboutEvent
    data object OnVersionClicked : AboutEvent
    data object OnChangelogClicked : AboutEvent
    data object OnChangelogDismissed : AboutEvent
    data object OnToastShown : AboutEvent
}