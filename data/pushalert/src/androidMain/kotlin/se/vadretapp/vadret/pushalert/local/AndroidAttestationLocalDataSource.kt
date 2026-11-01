package se.vadretapp.vadret.pushalert.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import se.vadretapp.vadret.pushalert.error.AttestationException
import se.vadretapp.vadret.pushalert.model.AndroidAttestation
import se.vadretapp.vadret.pushalert.model.Attestation
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.ProviderException
import java.security.spec.ECGenParameterSpec

internal class AndroidAttestationLocalDataSource(
    private val ioDispatcher: CoroutineDispatcher
) : AttestationLocalDataSource {
    override suspend fun attest(challenge: ByteArray): Attestation = withContext(context = ioDispatcher) {
        try {
            AndroidAttestation(certificateChain = attestedCertificateChain(challenge = challenge))
        } catch (exception: GeneralSecurityException) {
            throw AttestationException(message = "The Keystore could not attest a key: ${exception.message}")
        } catch (exception: ProviderException) {
            throw AttestationException(message = "The Keystore could not make an attested key: ${exception.message}")
        } catch (exception: IOException) {
            throw AttestationException(message = "The Keystore could not be opened: ${exception.message}")
        }
    }

    private fun attestedCertificateChain(challenge: ByteArray): List<ByteArray> {
        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEY_STORE)
        keyPairGenerator.initialize(
            KeyGenParameterSpec.Builder(ATTESTATION_KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec(P256_CURVE))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAttestationChallenge(challenge)
                .build()
        )
        keyPairGenerator.generateKeyPair()
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        val certificateChain = keyStore.getCertificateChain(ATTESTATION_KEY_ALIAS)
            ?: throw AttestationException(message = "The Keystore made a key without a certificate chain")
        keyStore.deleteEntry(ATTESTATION_KEY_ALIAS)
        return certificateChain.map { certificate -> certificate.encoded }
    }
}

private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val ATTESTATION_KEY_ALIAS = "se.vadretapp.vadret.pushalert.attestation"
private const val P256_CURVE = "secp256r1"