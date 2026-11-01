package se.vadretapp.vadret.common.extension

import kotlinx.io.IOException
import java.net.UnknownHostException

internal actual fun IOException.isNetworkUnavailable(): Boolean = this is UnknownHostException