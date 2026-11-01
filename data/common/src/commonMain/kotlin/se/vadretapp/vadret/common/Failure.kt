package se.vadretapp.vadret.common

data class Failure(val errorCode: ErrorCode) {

    companion object {
        fun unknown(): Failure = Failure(errorCode = ErrorCode.UNKNOWN)
    }
}