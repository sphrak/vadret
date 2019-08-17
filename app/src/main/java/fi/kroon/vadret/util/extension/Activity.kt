package fi.kroon.vadret.util.extension

import android.app.Activity
import fi.kroon.vadret.BaseApplication
import fi.kroon.vadret.di.component.CoreApplicationComponent

fun Activity.appComponent(): CoreApplicationComponent =
    BaseApplication
        .appComponent(this)