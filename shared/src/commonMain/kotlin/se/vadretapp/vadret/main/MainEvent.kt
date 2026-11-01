package se.vadretapp.vadret.main

internal sealed interface MainEvent {

    data object OnStart : MainEvent
}