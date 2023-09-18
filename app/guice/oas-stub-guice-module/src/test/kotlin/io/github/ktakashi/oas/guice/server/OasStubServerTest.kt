package io.github.ktakashi.oas.guice.server

import io.github.ktakashi.oas.guice.configurations.OasStubConfiguration
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceServerConfiguration
import io.github.ktakashi.oas.guice.injector.createServerInjector
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import org.eclipse.jetty.server.Server
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class OasStubServerTest {
    companion object {
        private lateinit var server: OasStubServer
        @JvmStatic
        @BeforeAll
        fun setup() {
            val configuration = OasStubGuiceServerConfiguration.builder()
                .oasStubConfiguration(OasStubConfiguration())
                .jettyServerSupplier(::Server)
                .build()
            val oasServerInjector = createServerInjector(configuration)
            server = oasServerInjector.getInstance(OasStubServer::class.java)
            server.start()

            RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            server.stop()
        }
    }

    @Test
    fun adminTest() {
        given().get("http://localhost:${server.port()}/__admin/petstore")
            .then()
            .statusCode(404)
        given().contentType(ContentType.JSON).body("{}")
            .post("http://localhost:${server.port()}/__admin/petstore")
            .then()
            .statusCode(201)

        given().get("http://localhost:${server.port()}/__admin")
            .then()
            .statusCode(200)
            .body("[0]", equalTo("petstore"))
    }

    @Test
    fun apiTest() {
        given().contentType(ContentType.TEXT).body("{}")
            .body(OasStubServerTest::class.java.getResourceAsStream("/schema/simple-api.yaml"))
            .post("http://localhost:${server.port()}/__admin/simple-api")
            .then()
            .statusCode(201)

        given().get("http://localhost:${server.port()}/oas/simple-api/hello")
            .then()
            .statusCode(200)
            .body("response-id", equalTo(10001))
            .body("message", equalTo("Hello"))
    }
}