package io.github.ktakashi.oas.test.http.glue

import io.cucumber.spring.CucumberContextConfiguration
import io.github.ktakashi.oas.test.AutoConfigureOasStubServer
import io.github.ktakashi.oas.test.OasStubServerPort
import io.github.ktakashi.oas.test.OasStubTestProperties
import io.github.ktakashi.oas.test.cucumber.TestContext
import io.github.ktakashi.oas.test.cucumber.TestContextSupplier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.test.annotation.DirtiesContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@CucumberContextConfiguration
@AutoConfigureOasStubServer(port = 0)
@DirtiesContext
class HttpConfiguration {
    @TestConfiguration
    class TestConfig {
        @Bean @Lazy
        fun testContextSupplier(@OasStubServerPort localPort: Int, properties: OasStubTestProperties) = TestContextSupplier {
            TestContext("http://localhost:$localPort", properties.server.stubPrefix, properties.server.adminPrefix)
        }
    }
}