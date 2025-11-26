package io.github.ktakashi.oas.test.ktor

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.engine.java.JavaHttpConfig
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.network.tls.addKeyStore
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import okhttp3.Protocol

fun <T: HttpClientEngineConfig> HttpClientEngineFactory<T>.createHttpClient(ks: KeyStore? = null, keyAlias: String? = null, keyPassword: String? = null) = HttpClient(this) {
    install(Logging) {
        level = LogLevel.ALL // make it configurable
    }
    engine {
        setupClientConfiguration(ks, keyAlias, keyPassword)
    }
}

private fun <T : HttpClientEngineConfig> T.setupClientConfiguration(ks: KeyStore?, keyAlias: String?, keyPassword: String?) {
    val trustManagerFactory = trustManagerFactory(ks)
    when (this) {
        // It didn't work as I expected on HTTPS requests...
        is JavaHttpConfig -> config {
            sslContext(sslContext(ks, keyPassword, trustManagerFactory))
        }
        // Currently, CIO only supports RSA and DSS which doesn't work on this
        is CIOEngineConfig -> https {
            if (ks != null) {
                trustManager = trustManagerFactory?.trustManagers?.filterIsInstance<X509TrustManager>()?.firstOrNull()
                if (keyAlias != null && keyPassword != null) {
                    addKeyStore(ks, keyPassword.toCharArray(), keyAlias)
                }
            }
        }
        is OkHttpConfig -> config {
            protocols(listOf(Protocol.HTTP_1_1))
            sslContext(ks, keyPassword, trustManagerFactory)?.socketFactory?.let { sslSocketFactory ->
                sslSocketFactory(sslSocketFactory, trustManagerFactory?.trustManagers!!.filterIsInstance<X509TrustManager>().first())
            }
        }
    }
}

private fun sslContext(ks: KeyStore?, password: String?, trustManagerFactory: TrustManagerFactory?) = keyManagerFactory(ks, password)?.let {
    SSLContext.getInstance("TLS").apply {
        init(it.keyManagers, trustManagerFactory?.trustManagers, null)
    }
}

private fun keyManagerFactory(ks: KeyStore?, password: String?): KeyManagerFactory? = if (ks != null && password != null) {
    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
        init(ks, password.toCharArray())
    }
} else {
    null
}

private fun trustManagerFactory(ks: KeyStore?): TrustManagerFactory? = ks?.let {
    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
        init(ks)
    }
}
