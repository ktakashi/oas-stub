package io.github.ktakashi.oas

import io.github.ktakashi.oas.server.OasStubServer
import io.github.ktakashi.oas.server.options.OasStubOptions
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PluginTest {
    companion object {
        lateinit var server: OasStubServer
        @JvmStatic
        @BeforeAll
        fun setup() {
            val options = OasStubOptions.builder()
                .serverOptions()
                .port(0)
                .parent()
                .stubOptions()
                .enableRecords(true)
                .addStaticConfiguration("classpath:/static-config.yaml")
                .parent()
                .build()
            server = OasStubServer(options)
            server.start()
        }
    }

    @Test
    fun staticPluginTest() {
        val r = given().get("http://localhost:${server.port()}/oas/test-api-static/examples")
            .then()
            .statusCode(404)
            .header("X-Example-Id", equalTo("12345"))
            .extract()
        val headers = r.headers().filter { it.name == "X-Example-Values" }
        assertEquals(3, headers.size)
        assertTrue(setOf("a", "b", "c").containsAll(headers.map { it.value }))

        val response = given().get("http://localhost:${server.port()}/__admin/metrics/test-api-static")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
        assertEquals(1, response.get<Map<String, List<Any>>>("metrics")["/examples"]?.size)
    }

    @Test
    fun listApiDataTest() {
        given().get("http://localhost:${server.port()}/oas/test-api-static/examples/objects")
            .then()
            .statusCode(200)
            .body("size()", equalTo(4))
            .body("[0]", equalTo("oas"))
            .body("[1]", equalTo("stub"))
            .body("[2]", equalTo("is"))
            .body("[3]", equalTo("great"))

        val response = given().get("http://localhost:${server.port()}/__admin/metrics/test-api-static")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
        assertEquals(1, response.get<Map<String, List<Any>>>("metrics")["/examples/objects"]?.size)
    }
}