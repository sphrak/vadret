package se.vadretapp.vadret.pushalert.model

enum class PollenAlertLevel(val level: Int) {
    LOW(level = 1),
    LOW_TO_MODERATE(level = 2),
    MODERATE(level = 3),
    MODERATE_TO_HIGH(level = 4),
    HIGH(level = 5),
    HIGH_TO_VERY_HIGH(level = 6),
    VERY_HIGH(level = 7)
}