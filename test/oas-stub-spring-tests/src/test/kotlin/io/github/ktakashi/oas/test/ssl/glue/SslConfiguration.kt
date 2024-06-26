package io.github.ktakashi.oas.test.ssl.glue

import io.cucumber.java.Before
import io.cucumber.spring.CucumberContextConfiguration
import io.github.ktakashi.oas.server.OasStubServer
import io.github.ktakashi.oas.test.AutoConfigureOasStubServer
import io.github.ktakashi.oas.test.OasStubServerHttpsPort
import io.github.ktakashi.oas.test.OasStubTestProperties
import io.github.ktakashi.oas.test.cucumber.TestContext
import io.github.ktakashi.oas.test.cucumber.TestContextSupplier
import io.restassured.RestAssured
import io.restassured.config.SSLConfig
import java.security.KeyStore
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.core.io.ClassPathResource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableAutoConfiguration
@CucumberContextConfiguration
@AutoConfigureOasStubServer(port = 0, httpsPort = 0)
class SslConfiguration(private val oasStubServer: OasStubServer) {
    @TestConfiguration
    class TestConfig {
        @Bean @Lazy
        fun testContextSupplier(@OasStubServerHttpsPort localPort: Int, properties: OasStubTestProperties) = TestContextSupplier {
            TestContext("https://localhost:$localPort", properties.server.stubPrefix, properties.server.adminPrefix)
        }
    }

    @Before
    fun init() {
        val ks = KeyStore.getInstance("JKS")
        ks.load(null)
        ks.setCertificateEntry("oas-stub", oasStubServer.certificate())
        val sslConfig = SSLConfig().trustStore(ks)
            .keyStore(ClassPathResource("/keystore.p12").file, "password")
            .keystoreType("PKCS12")
        RestAssured.config = RestAssured.config().sslConfig(sslConfig)
    }
}