@file:OptIn(ExperimentalForeignApi::class)

package se.vadretapp.vadret.encryption

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.Security.SecKeyCreateWithData
import platform.Security.SecKeyRef
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPrivate
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import se.vadretapp.vadret.encryption.error.EncryptionError
import se.vadretapp.vadret.encryption.extension.toCFData
import se.vadretapp.vadret.encryption.model.KeyAlias
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals

class SecureEnclaveSealedMessageKeyTest {
    @Test
    fun aMessageSealedByTheServerOpensWithTheKeyItWasSealedFor() {
        assertEquals(
            expected = PLAINTEXT,
            actual = sealedMessageKey(privateKeySource = ImportedPrivateKeySource()).open(sealed = SEALED).rightOrNull()?.decodeToString()
        )
    }

    @Test
    fun thePublicKeyIsTheUncompressedPointTheServerSealsFor() {
        assertEquals(
            expected = PUBLIC_POINT.toList(),
            actual = sealedMessageKey(privateKeySource = ImportedPrivateKeySource()).publicKey().rightOrNull()?.toList()
        )
    }

    @Test
    fun aTamperedMessageIsNotForThisKey() {
        val tampered = SEALED.copyOf()
        tampered[tampered.lastIndex] = (tampered.last() + 1).toByte()

        assertEquals(
            expected = EncryptionError.NOT_FOR_THIS_KEY.asLeft(),
            actual = sealedMessageKey(privateKeySource = ImportedPrivateKeySource()).open(sealed = tampered)
        )
    }

    @Test
    fun aKeyTheKeychainCannotGiveOpensNothing() {
        val unavailable = PrivateKeySource { EncryptionError.KEY_STORE_UNAVAILABLE.asLeft() }

        assertEquals(
            expected = listOf(EncryptionError.KEY_STORE_UNAVAILABLE.asLeft(), EncryptionError.KEY_STORE_UNAVAILABLE.asLeft()),
            actual = listOf(
                sealedMessageKey(privateKeySource = unavailable).open(sealed = SEALED),
                sealedMessageKey(privateKeySource = unavailable).publicKey()
            )
        )
    }
}

private class ImportedPrivateKeySource : PrivateKeySource {
    override fun privateKey(): Either<EncryptionError, SecKeyRef> = memScoped {
        val attributes = cfDictionaryOf(
            entries = listOf<Pair<CFStringRef?, CFTypeRef?>>(
                kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
                kSecAttrKeyClass to kSecAttrKeyClassPrivate
            )
        )
        val keyData = X963_PRIVATE_KEY.toCFData()
        val error = alloc<CFErrorRefVar>()
        val privateKey = checkNotNull(value = SecKeyCreateWithData(keyData, attributes, error.ptr)) { "The test key could not be imported" }
        CFRelease(keyData)
        CFRelease(attributes)
        privateKey.asRight()
    }
}

private fun sealedMessageKey(privateKeySource: PrivateKeySource): SealedMessageKey =
    SecureEnclaveSealedMessageKey(
        keyAlias = KeyAlias(value = "se.vadretapp.vadret.test"),
        privateKeySource = privateKeySource
    )

private fun <T> Either<EncryptionError, T>.rightOrNull(): T? = when (this) {
    is Either.Left -> null
    is Either.Right -> b
}

private val X963_PRIVATE_KEY = Base64.decode(source = "BGJ0AnQQ0CUq9hUYqDeemnaWjR3FOgDQ/hyTKoCRyPqPBkYcAzinUYMMknvB8JXepWnVCp7Gta/q8tFzOK+8l7IPBHE2QMIEJRH4X0ArEUw7bIVKXS+USXEDWe5kGR7XqA==")
private val PUBLIC_POINT = Base64.decode(source = "BGJ0AnQQ0CUq9hUYqDeemnaWjR3FOgDQ/hyTKoCRyPqPBkYcAzinUYMMknvB8JXepWnVCp7Gta/q8tFzOK+8l7I=")
private val SEALED = Base64.decode(source = "BEMSv8COK/25apL9e87hhBcXx66VlKiscNL99uErOTiF2IRx/1DQumVsMFWAabHkPx1Y4gtZsPIZqzK6wxmc0Sw7ODxlo8Fb0A0uFa0hQBOHT5jIiKKMMEUeXczRgbTOrwpCKX121A2ywEcku0kHi+UnRtNeIznsozTLwK9vknrBNTq8UbexGhuBv+EiRj1DT9VYt4uBrufZ5o3Ws40u0QtxODmyitWoMYMSHPdMdMlIrItscpcqGiPgQg==")
private const val PLAINTEXT = "{\"title\":\"Gul varning: Stockholms län\",\"body\":\"Kuling\",\"data\":{\"kind\":\"warning\",\"notification_id\":\"n-1\"}}"