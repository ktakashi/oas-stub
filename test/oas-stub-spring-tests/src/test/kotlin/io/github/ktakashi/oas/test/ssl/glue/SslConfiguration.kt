package io.github.ktakashi.oas.test.ssl.glue

import io.cucumber.spring.CucumberContextConfiguration
import io.github.ktakashi.oas.server.OasStubServer
import io.github.ktakashi.oas.test.AutoConfigureOasStubServer
import io.github.ktakashi.oas.test.OasStubServerHttpsPort
import io.github.ktakashi.oas.test.OasStubTestProperties
import io.github.ktakashi.oas.test.cucumber.TestContext
import io.github.ktakashi.oas.test.cucumber.TestContextSupplier
import io.github.ktakashi.oas.test.ktor.createHttpClient
import io.ktor.client.engine.cio.CIO
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
class SslConfiguration {
    @TestConfiguration
    class TestConfig(private val oasStubServer: OasStubServer) {
        @Bean @Lazy
        fun testContextSupplier(@OasStubServerHttpsPort localPort: Int, properties: OasStubTestProperties) = TestContextSupplier {
            TestContext("https://localhost:$localPort", properties.server.stubPrefix, properties.server.adminPrefix)
        }

        @Bean
        fun keyStore(): KeyStore = KeyStore.getInstance("PKCS12").apply {
            load(ClassPathResource("/keystore.p12").inputStream, "password".toCharArray())
        }
    }
}