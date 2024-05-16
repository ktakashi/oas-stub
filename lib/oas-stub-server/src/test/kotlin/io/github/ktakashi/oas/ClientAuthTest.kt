package io.github.ktakashi.oas

import io.github.ktakashi.oas.server.OasStubServer
import io.github.ktakashi.oas.server.options.OasStubOptions
import io.github.ktakashi.oas.server.options.OasStubServerSSLOptions
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.config.SSLConfig
import java.security.KeyStore
import javax.net.ssl.SSLException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ClientAuthTest {
    companion object {
        private lateinit var server: OasStubServer
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
            val sslConfig = SSLConfig().trustStore(ks)
            RestAssured.config = RestAssured.config().sslConfig(sslConfig)
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
            given().get("https://localhost:${server.httpsPort()}/${server.adminPath()}")
        }
    }
}