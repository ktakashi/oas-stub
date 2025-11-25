package io.github.ktakashi.oas.test.ktor

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.network.tls.addKeyStore
import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

fun CIO.createHttpClient(ks: KeyStore? = null, keyAlias: String? = null, password: String? = null) = HttpClient(this) {
    install(Logging) {
        level = LogLevel.ALL // make it configurable
    }
    engine {
        https {
            if (ks != null) {
                trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                    init(ks)
                }?.trustManagers?.first { it is X509TrustManager }
                if (keyAlias != null && password != null) {
                    addKeyStore(ks, password.toCharArray(), keyAlias)
                }
            }
        }
    }
}