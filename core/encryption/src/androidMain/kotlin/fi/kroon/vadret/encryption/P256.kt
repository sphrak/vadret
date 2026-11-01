package fi.kroon.vadret.encryption

import java.security.AlgorithmParameters
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec

internal val p256: ECParameterSpec = p256Parameters()

private fun p256Parameters(): ECParameterSpec {
    val parameters = AlgorithmParameters.getInstance(EC_ALGORITHM)
    parameters.init(ECGenParameterSpec(P256_CURVE))
    return parameters.getParameterSpec(ECParameterSpec::class.java)
}

internal const val EC_ALGORITHM = "EC"
internal const val P256_CURVE = "secp256r1"
internal const val UNCOMPRESSED_POINT_BYTES = 65
internal const val COORDINATE_BYTES = 32
internal const val UNCOMPRESSED_POINT_PREFIX: Byte = 0x04