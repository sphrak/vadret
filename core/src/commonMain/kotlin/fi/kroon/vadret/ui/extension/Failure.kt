package fi.kroon.vadret.ui.extension

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.core.resources.Res
import fi.kroon.vadret.core.resources.attestation_failed
import fi.kroon.vadret.core.resources.attestation_refused
import fi.kroon.vadret.core.resources.data_not_available_at_this_location
import fi.kroon.vadret.core.resources.failed_loading_file
import fi.kroon.vadret.core.resources.http_bad_request_400
import fi.kroon.vadret.core.resources.http_forbidden_403
import fi.kroon.vadret.core.resources.http_gateway_timeout_504
import fi.kroon.vadret.core.resources.http_internal_server_error_500
import fi.kroon.vadret.core.resources.http_service_unavailable_503
import fi.kroon.vadret.core.resources.http_unauthorized_401
import fi.kroon.vadret.core.resources.location_failure
import fi.kroon.vadret.core.resources.location_provider_disabled
import fi.kroon.vadret.core.resources.network_failure
import fi.kroon.vadret.core.resources.no_location_permission
import fi.kroon.vadret.core.resources.no_network_available
import fi.kroon.vadret.core.resources.no_radar_available
import fi.kroon.vadret.core.resources.no_wind_available
import fi.kroon.vadret.core.resources.push_alerts_only_in_sweden_text
import fi.kroon.vadret.core.resources.unhandled_error
import org.jetbrains.compose.resources.StringResource

fun Failure.messageResource(): StringResource =
    when (errorCode) {
        ErrorCode.NETWORK_UNAVAILABLE -> Res.string.no_network_available
        ErrorCode.NETWORK_FAILURE,
        ErrorCode.HTTP_NOT_FOUND,
        ErrorCode.HTTP_UNEXPECTED_STATUS,
        ErrorCode.INVALID_RESPONSE -> Res.string.network_failure
        ErrorCode.HTTP_BAD_REQUEST -> Res.string.http_bad_request_400
        ErrorCode.HTTP_UNAUTHORIZED -> Res.string.http_unauthorized_401
        ErrorCode.HTTP_FORBIDDEN -> Res.string.http_forbidden_403
        ErrorCode.HTTP_INTERNAL_SERVER_ERROR -> Res.string.http_internal_server_error_500
        ErrorCode.HTTP_SERVICE_UNAVAILABLE -> Res.string.http_service_unavailable_503
        ErrorCode.HTTP_GATEWAY_TIMEOUT -> Res.string.http_gateway_timeout_504
        ErrorCode.LOCATION_PROVIDER_DISABLED -> Res.string.location_provider_disabled
        ErrorCode.LOCATION_NOT_AVAILABLE -> Res.string.location_failure
        ErrorCode.LOCATION_PERMISSION_MISSING -> Res.string.no_location_permission
        ErrorCode.FILE_READ_FAILED -> Res.string.failed_loading_file
        ErrorCode.WEATHER_FORECAST_NOT_AVAILABLE_FOR_LOCATION,
        ErrorCode.WEATHER_ANALYSIS_NOT_AVAILABLE_FOR_LOCATION -> Res.string.data_not_available_at_this_location
        ErrorCode.RADAR_NOT_AVAILABLE -> Res.string.no_radar_available
        ErrorCode.WIND_NOT_AVAILABLE -> Res.string.no_wind_available
        ErrorCode.ATTESTATION_FAILED -> Res.string.attestation_failed
        ErrorCode.ATTESTATION_REFUSED -> Res.string.attestation_refused
        ErrorCode.UNSUPPORTED_AREA -> Res.string.push_alerts_only_in_sweden_text
        ErrorCode.PUSH_ALERT_NOT_CHOSEN,
        ErrorCode.STORAGE_FAILED,
        ErrorCode.UNKNOWN -> Res.string.unhandled_error
    }