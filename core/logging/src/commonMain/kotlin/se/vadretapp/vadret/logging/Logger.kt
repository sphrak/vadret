package se.vadretapp.vadret.logging

interface Logger {
    fun debug(message: () -> String)
    fun info(message: () -> String)
    fun warn(message: () -> String)
    fun error(message: () -> String)
}