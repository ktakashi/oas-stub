package io.github.ktakashi.oas.test.server

import io.github.ktakashi.oas.model.ApiData
import io.github.ktakashi.oas.model.ApiHeaders
import io.github.ktakashi.oas.test.OasStubTestResources
import io.github.ktakashi.oas.test.OasStubTestService
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import java.net.URI
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest

@SpringBootApplication
class OasServerApplication

private fun check(scheme: String, httpPort: Int, oasStubTestService: OasStubTestService) {
    RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())

    given().get(URI.create("${scheme}://localhost:$httpPort/oas/petstore/v1/pets/1"))
        .then()
        .statusCode(200)
        .body("id", equalTo(1))
    assertEquals(1, oasStubTestService.getTestApiMetrics("petstore").byPath("/v1/pets/1").count())

    given().get(URI.create("${scheme}://localhost:$httpPort/oas/petstore/v1/pets/2"))
        .then()
        .statusCode(404)
        .body("message", equalTo("No pet found"))
    assertEquals(1, oasStubTestService.getTestApiMetrics("petstore").byStatus(200).count())
    assertEquals(1, oasStubTestService.getTestApiMetrics("petstore").byStatus(404).count())
    assertEquals(2, oasStubTestService.getTestApiMetrics("petstore").filter { m -> m.httpMethod == "GET"}.count())

    val context = oasStubTestService.getTestApiContext("petstore")
        .updateHeaders(ApiHeaders(response = sortedMapOf("Extra-Header" to listOf("extra-value"))))
    context.getApiConfiguration("/v1/pets/{id}").ifPresent { config ->
        val value = OasStubTestResources.DefaultResponseModel(status = 200, response = """{"id": 2,"name": "Pochi","tag": "dog"}""")
        val map = config.data?.asMap()?.toMutableMap()
        map?.put("/v1/pets/2", value)
        context.updateApi("/v1/pets/{id}", config.updateData(ApiData(map!!))).save()
    }

    given().get(URI.create("${scheme}://localhost:$httpPort/oas/petstore/v1/pets/2"))
        .then()
        .statusCode(200)
        .header("Extra-Header", equalTo("extra-value"))
        .body("id", equalTo(2))
}

@SpringBootTest
@AutoConfigureOasStubServer(port = 0, httpsPort = 0)
class OasServerApplicationTest(@Autowired private val oasStubTestService: OasStubTestService,
                               @Value("\${oas.stub.test.server.port}") private val httpPort: Int,
                               @Value("\${oas.stub.test.server.https-port}") private val httpsPort: Int) {
    @Test
    fun test() {
        assertTrue(httpPort > 0)
        assertTrue(httpsPort > 0)

        check("http", httpPort, oasStubTestService)
    }

    @Test
    fun testHttps() {
        assertTrue(httpPort > 0)
        assertTrue(httpsPort > 0)

        check("https", httpsPort, oasStubTestService)
    }
}
