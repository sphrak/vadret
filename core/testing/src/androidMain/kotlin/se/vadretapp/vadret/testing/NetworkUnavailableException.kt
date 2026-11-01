package se.vadretapp.vadret.testing

import java.net.UnknownHostException

actual fun networkUnavailableException(host: String): Exception = UnknownHostException(host)