package io.github.ktakashi.oas.test.http.glue

import io.cucumber.spring.CucumberContextConfiguration
import io.github.ktakashi.oas.test.AutoConfigureOasStubServer
import io.github.ktakashi.oas.test.OasStubTestProperties
import io.github.ktakashi.oas.test.cucumber.TestContext
import io.github.ktakashi.oas.test.cucumber.TestContextSupplier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableAutoConfiguration
@CucumberContextConfiguration
@AutoConfigureOasStubServer(port = 0)
class HttpConfiguration {
    @TestConfiguration
    class TestConfig {
        @Bean @Lazy
        fun testContextSupplier(@Value("\${${OasStubTestProperties.OAS_STUB_SERVER_PROPERTY_PREFIX}.port}") localPort: Int, properties: OasStubTestProperties) = TestContextSupplier {
            TestContext("http://localhost:$localPort", properties.server.stubPrefix, properties.server.adminPrefix)
        }
    }
}