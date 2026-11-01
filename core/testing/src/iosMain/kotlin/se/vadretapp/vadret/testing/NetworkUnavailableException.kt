package se.vadretapp.vadret.testing

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorFailingURLStringErrorKey
import platform.Foundation.NSURLErrorNotConnectedToInternet

actual fun networkUnavailableException(host: String): Exception =
    DarwinHttpRequestException(
        origin = NSError(
            domain = checkNotNull(value = NSURLErrorDomain),
            code = NSURLErrorNotConnectedToInternet,
            userInfo = mapOf(NSURLErrorFailingURLStringErrorKey to host)
        )
    )