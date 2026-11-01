package fi.kroon.vadret.encryption.extension

import fi.kroon.vadret.encryption.COORDINATE_BYTES
import fi.kroon.vadret.encryption.EC_ALGORITHM
import fi.kroon.vadret.encryption.UNCOMPRESSED_POINT_BYTES
import fi.kroon.vadret.encryption.UNCOMPRESSED_POINT_PREFIX
import fi.kroon.vadret.encryption.p256
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.ECFieldFp
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec

internal fun ByteArray.toP256PublicKeyOrNull(): ECPublicKey? {
    if (size != UNCOMPRESSED_POINT_BYTES || first() != UNCOMPRESSED_POINT_PREFIX) {
        return null
    }
    val x = BigInteger(1, copyOfRange(fromIndex = 1, toIndex = 1 + COORDINATE_BYTES))
    val y = BigInteger(1, copyOfRange(fromIndex = 1 + COORDINATE_BYTES, toIndex = UNCOMPRESSED_POINT_BYTES))
    if (!isOnP256(x = x, y = y)) {
        return null
    }
    return KeyFactory.getInstance(EC_ALGORITHM).generatePublic(ECPublicKeySpec(ECPoint(x, y), p256)) as ECPublicKey
}

private fun isOnP256(
    x: BigInteger,
    y: BigInteger
): Boolean {
    val prime = (p256.curve.field as ECFieldFp).p
    if (x.signum() < 0 || x >= prime || y.signum() < 0 || y >= prime) {
        return false
    }
    val left = y.modPow(SQUARE, prime)
    val right = x.modPow(CUBE, prime).add(p256.curve.a.multiply(x)).add(p256.curve.b).mod(prime)
    return left == right
}

private val SQUARE: BigInteger = BigInteger.valueOf(2L)
private val CUBE: BigInteger = BigInteger.valueOf(3L)