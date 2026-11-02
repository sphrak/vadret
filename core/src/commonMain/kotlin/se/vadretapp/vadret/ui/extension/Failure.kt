package se.vadretapp.vadret.ui.extension

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.attestation_failed
import se.vadretapp.vadret.core.resources.attestation_refused
import se.vadretapp.vadret.core.resources.data_not_available_at_this_location
import se.vadretapp.vadret.core.resources.failed_loading_file
import se.vadretapp.vadret.core.resources.http_bad_request_400
import se.vadretapp.vadret.core.resources.http_forbidden_403
import se.vadretapp.vadret.core.resources.http_gateway_timeout_504
import se.vadretapp.vadret.core.resources.http_internal_server_error_500
import se.vadretapp.vadret.core.resources.http_service_unavailable_503
import se.vadretapp.vadret.core.resources.http_unauthorized_401
import se.vadretapp.vadret.core.resources.location_failure
import se.vadretapp.vadret.core.resources.location_provider_disabled
import se.vadretapp.vadret.core.resources.network_failure
import se.vadretapp.vadret.core.resources.no_location_permission
import se.vadretapp.vadret.core.resources.no_network_available
import se.vadretapp.vadret.core.resources.no_radar_available
import se.vadretapp.vadret.core.resources.no_wind_available
import se.vadretapp.vadret.core.resources.push_alerts_only_in_sweden_text
import se.vadretapp.vadret.core.resources.unhandled_error

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
        ErrorCode.FAVORITE_NAME_TAKEN,
        ErrorCode.STORAGE_FAILED,
        ErrorCode.UNKNOWN -> Res.string.unhandled_error
    }