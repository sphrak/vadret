@file:OptIn(ExperimentalForeignApi::class)

package fi.kroon.vadret.encryption

import cnames.structs.__SecKey
import fi.kroon.vadret.encryption.error.EncryptionError
import fi.kroon.vadret.encryption.extension.describeAndRelease
import fi.kroon.vadret.encryption.extension.toCFData
import fi.kroon.vadret.encryption.model.KeyAlias
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberSInt32Type
import platform.Foundation.NSLock
import platform.Security.SecAccessControlCreateWithFlags
import platform.Security.SecAccessControlRef
import platform.Security.SecItemCopyMatching
import platform.Security.SecKeyCreateRandomKey
import platform.Security.SecKeyRef
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAccessControlPrivateKeyUsage
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrApplicationTag
import platform.Security.kSecAttrIsPermanent
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPrivate
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecAttrTokenID
import platform.Security.kSecAttrTokenIDSecureEnclave
import platform.Security.kSecClass
import platform.Security.kSecClassKey
import platform.Security.kSecPrivateKeyAttrs
import platform.Security.kSecReturnRef

internal class KeychainPrivateKeySource(
    private val keyAlias: KeyAlias
) : PrivateKeySource {
    private val keychainLock = NSLock()

    override fun privateKey(): Either<EncryptionError, SecKeyRef> {
        keychainLock.lock()
        val privateKey = findOrCreate()
        keychainLock.unlock()
        return privateKey
    }

    private fun findOrCreate(): Either<EncryptionError, SecKeyRef> {
        val tag = keyAlias.value.encodeToByteArray().toCFData() ?: return unavailable(description = "the key tag could not be made")
        val privateKey = when (val found = find(tag = tag)) {
            is Either.Left -> found
            is Either.Right -> found.b?.asRight() ?: create(tag = tag)
        }
        CFRelease(tag)
        return privateKey
    }

    private fun find(tag: CFDataRef): Either<EncryptionError, SecKeyRef?> = memScoped {
        val query = cfDictionaryOf(
            entries = listOf(
                kSecClass to kSecClassKey,
                kSecAttrKeyClass to kSecAttrKeyClassPrivate,
                kSecAttrApplicationTag to tag,
                kSecReturnRef to kCFBooleanTrue
            )
        )
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        CFRelease(query)
        when (status) {
            errSecSuccess -> result.value?.reinterpret<__SecKey>().asRight()
            errSecItemNotFound -> null.asRight()
            else -> unavailable(description = "the Keychain answered $status when asked for the key")
        }
    }

    private fun create(tag: CFDataRef): Either<EncryptionError, SecKeyRef> = memScoped {
        val keyBits = alloc<IntVar>()
        keyBits.value = P256_KEY_BITS
        val keySize = CFNumberCreate(kCFAllocatorDefault, kCFNumberSInt32Type, keyBits.ptr)
        val accessControl = secureEnclaveAccessControl()
        val privateKeyAttributes = cfDictionaryOf(
            entries = listOf(
                kSecAttrIsPermanent to kCFBooleanTrue,
                kSecAttrApplicationTag to tag,
                protection(accessControl = accessControl)
            )
        )
        val attributes = cfDictionaryOf(
            entries = listOfNotNull(
                kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
                kSecAttrKeySizeInBits to keySize,
                kSecPrivateKeyAttrs to privateKeyAttributes,
                secureEnclaveToken()
            )
        )
        val error = alloc<CFErrorRefVar>()
        val privateKey = SecKeyCreateRandomKey(attributes, error.ptr)
        CFRelease(attributes)
        CFRelease(privateKeyAttributes)
        CFRelease(keySize)
        if (accessControl != null) {
            CFRelease(accessControl)
        }
        if (privateKey == null) {
            unavailable(description = "no key could be made: ${error.value.describeAndRelease()}")
        } else {
            privateKey.asRight()
        }
    }

    private fun unavailable(description: String): Either<EncryptionError, Nothing> {
        logger.warn { "${EncryptionError.KEY_STORE_UNAVAILABLE}: ${keyAlias.value}: $description" }
        return EncryptionError.KEY_STORE_UNAVAILABLE.asLeft()
    }
}

private fun secureEnclaveAccessControl(): SecAccessControlRef? =
    if (isSecureEnclaveAvailable) {
        SecAccessControlCreateWithFlags(kCFAllocatorDefault, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly, kSecAccessControlPrivateKeyUsage, null)
    } else {
        null
    }

private fun protection(accessControl: SecAccessControlRef?): Pair<CFStringRef?, CFTypeRef?> =
    if (accessControl == null) {
        kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
    } else {
        kSecAttrAccessControl to accessControl
    }

private fun secureEnclaveToken(): Pair<CFStringRef?, CFTypeRef?>? =
    if (isSecureEnclaveAvailable) {
        kSecAttrTokenID to kSecAttrTokenIDSecureEnclave
    } else {
        null
    }

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.encryption")

private const val P256_KEY_BITS = 256