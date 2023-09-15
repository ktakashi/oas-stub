package io.github.ktakashi.oas.guice.server

import io.github.ktakashi.oas.guice.configurations.OasStubGuiceServerConfiguration
import io.github.ktakashi.oas.guice.injector.createServerInjector
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class OasStubServerTest {
    companion object {
        private lateinit var server: OasStubServer
        @JvmStatic
        @BeforeAll
        fun setup() {
            val configuration = OasStubGuiceServerConfiguration.builder().build()
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
    }
}