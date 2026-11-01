package se.vadretapp.vadret.common.extension

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import kotlinx.io.IOException
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNotConnectedToInternet

private val unavailableErrorCodes: Set<Long> = setOf(
    NSURLErrorCannotFindHost,
    NSURLErrorDNSLookupFailed,
    NSURLErrorNotConnectedToInternet
)

internal actual fun IOException.isNetworkUnavailable(): Boolean =
    this is DarwinHttpRequestException &&
        origin.domain == NSURLErrorDomain &&
        origin.code in unavailableErrorCodes