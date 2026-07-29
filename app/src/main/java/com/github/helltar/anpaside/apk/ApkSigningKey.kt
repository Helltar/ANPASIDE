package com.github.helltar.anpaside.apk

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Calendar
import javax.security.auth.x500.X500Principal

data class SigningIdentity(
    val privateKey: PrivateKey,
    val certificate: X509Certificate
)

/**
 * The key exported apks are signed with.
 *
 * Android installs nothing unsigned and there is no keytool on a phone, so the key is generated
 * once in the platform key store and stays there. It is a local identity and nothing more:
 * it says only that two apks were exported from the same installation of the ide, which is
 * exactly what Android needs to let one replace the other. Uninstalling the ide takes the key
 * with it, and an already exported app then has to be uninstalled before a new export of it
 * can be installed.
 */
class ApkSigningKey(private val alias: String = DEFAULT_ALIAS) {

    fun identity(): SigningIdentity {
        if (!keyStore().containsAlias(alias)) {
            generate()
        }

        val keyStore = keyStore()

        return SigningIdentity(
            privateKey = keyStore.getKey(alias, null) as PrivateKey,
            certificate = keyStore.getCertificate(alias) as X509Certificate
        )
    }

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    private fun generate() {
        val notBefore = Calendar.getInstance()
        val notAfter = Calendar.getInstance().apply { add(Calendar.YEAR, VALIDITY_YEARS) }

        val specification =
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .setKeySize(KEY_SIZE)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                // the signer picks the scheme, so both paddings it can ask an rsa key for
                // have to be allowed here
                .setSignaturePaddings(
                    KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
                    KeyProperties.SIGNATURE_PADDING_RSA_PSS
                )
                .setCertificateSubject(X500Principal(SUBJECT))
                .setCertificateSerialNumber(BigInteger.ONE)
                .setCertificateNotBefore(notBefore.time)
                .setCertificateNotAfter(notAfter.time)
                .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE).run {
            initialize(specification)
            generateKeyPair()
        }
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val DEFAULT_ALIAS = "anpaside-export"
        const val SUBJECT = "CN=ANPASIDE, OU=Exported MIDlet"
        const val KEY_SIZE = 2048
        const val VALIDITY_YEARS = 100
    }
}
