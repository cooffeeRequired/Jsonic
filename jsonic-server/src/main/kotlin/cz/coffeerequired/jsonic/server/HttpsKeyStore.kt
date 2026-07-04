package cz.coffeerequired.jsonic.server

import java.io.File
import java.io.FileInputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

object HttpsKeyStore {

    fun load(certFile: File, keyFile: File, password: CharArray): KeyStore? {
        if (certFile.extension.equals("p12", ignoreCase = true) ||
            certFile.extension.equals("pkcs12", ignoreCase = true)
        ) {
            return KeyStore.getInstance("PKCS12").apply {
                FileInputStream(certFile).use { load(it, password) }
            }
        }
        return loadFromPem(certFile, keyFile, password)
    }

    private fun loadFromPem(certFile: File, keyFile: File, password: CharArray): KeyStore? {
        val certFactory = CertificateFactory.getInstance("X.509")
        val certificate = certFactory.generateCertificate(certFile.inputStream())
        val privateKey = parsePrivateKey(keyFile.readText()) ?: return null
        val store = KeyStore.getInstance(KeyStore.getDefaultType())
        store.load(null, null)
        store.setKeyEntry("0", privateKey, password, arrayOf(certificate))
        return store
    }

    private fun parsePrivateKey(pem: String): PrivateKey? {
        val normalized = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        if (normalized.isEmpty()) return null
        val decoded = Base64.getDecoder().decode(normalized)
        return try {
            KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(decoded))
        } catch (_: Exception) {
            null
        }
    }
}
