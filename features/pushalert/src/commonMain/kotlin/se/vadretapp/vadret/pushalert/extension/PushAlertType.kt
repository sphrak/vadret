package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.icons.Aurora
import se.vadretapp.vadret.icons.Colder
import se.vadretapp.vadret.icons.HardWind
import se.vadretapp.vadret.icons.Hurricane
import se.vadretapp.vadret.icons.Pollen
import se.vadretapp.vadret.icons.Slipperiness
import se.vadretapp.vadret.icons.Storm
import se.vadretapp.vadret.icons.VadretIconGraphic
import se.vadretapp.vadret.icons.VadretIcons
import se.vadretapp.vadret.icons.Warmer
import se.vadretapp.vadret.icons.Warning
import se.vadretapp.vadret.pushalert.model.PushAlertType

// The same icon the notification shows for this type.
internal fun PushAlertType.icon(): VadretIconGraphic =
    when (this) {
        PushAlertType.WARNING -> VadretIcons.Alert.Warning
        PushAlertType.POLLEN -> VadretIcons.Alert.Pollen
        PushAlertType.WARMER -> VadretIcons.Alert.Warmer
        PushAlertType.COLDER -> VadretIcons.Alert.Colder
        PushAlertType.HARD_WIND -> VadretIcons.Alert.HardWind
        PushAlertType.STORM -> VadretIcons.Alert.Storm
        PushAlertType.HURRICANE -> VadretIcons.Alert.Hurricane
        PushAlertType.SLIPPERINESS -> VadretIcons.Alert.Slipperiness
        PushAlertType.AURORA -> VadretIcons.Alert.Aurora
    }