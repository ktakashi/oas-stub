package io.github.ktakashi.oas

import io.github.ktakashi.oas.server.OasStubServer
import io.github.ktakashi.oas.server.options.OasStubOptions
import io.github.ktakashi.oas.server.options.OasStubServerSSLOptions
import io.github.ktakashi.oas.test.ktor.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import java.security.KeyStore
import javax.net.ssl.SSLException
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ClientAuthTest {
    companion object {
        private lateinit var server: OasStubServer
        private lateinit var client: HttpClient
        @BeforeAll
        @JvmStatic
        fun init() {
            server = OasStubServer(OasStubOptions.builder()
                .serverOptions()
                .httpsPort(0)
                .ssl().clientAuth(OasStubServerSSLOptions.ClientAuth.REQUIRE)
                .parent().parent().build())
            server.start()
            val ks = KeyStore.getInstance("JKS")
            ks.load(null)
            ks.setCertificateEntry("oas-stub", server.certificate())
            client = CIO.createHttpClient(ks)
        }

        @AfterAll
        @JvmStatic
        fun clean() {
            server.stop()
        }
    }

    @Test
    fun `ssl error on no client certificate`() {
        assertThrows<SSLException> {
            runBlocking {
                client.get("https://localhost:${server.httpsPort()}/${server.adminPath()}")
            }
        }
    }
}