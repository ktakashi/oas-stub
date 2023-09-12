package io.github.ktakashi.oas.example.test

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
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
class ExampleApplicationTest(@Value("\${local.server.port}") private val localPort: Int,
                             @Autowired private val oasStubTestService: OasStubTestService) {
    @Test
    fun check() {
        RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())

        given().get(URI.create("http://localhost:$localPort/oas/petstore/v1/pets/1"))
                .then()
                .statusCode(200)
                .body("id", equalTo(1))
        assertEquals(1, oasStubTestService.getTestApiMetrics("petstore").countBy("/v1/pets/1").get())

        given().get(URI.create("http://localhost:$localPort/oas/petstore/v1/pets/2"))
                .then()
                .statusCode(404)
                .body("message", equalTo("No pet found"))
        assertEquals(1, oasStubTestService.getTestApiMetrics("petstore").countBy(200).get())
        assertEquals(1, oasStubTestService.getTestApiMetrics("petstore").countBy(404).get())

        val context = oasStubTestService.getTestApiContext("petstore")
                .updateHeaders(ApiHeaders(response = sortedMapOf("Extra-Header" to listOf("extra-value"))))
        context.getApiConfiguration("/v1/pets/{id}").ifPresent { config ->
            val value = OasStubTestResources.DefaultResponseModel(status = 200, response = """{"id": 2,"name": "Pochi","tag": "dog"}""")
            val map = config.data?.asMap()?.toMutableMap()
            map?.put("/v1/pets/2", value)
            context.updateApi("/v1/pets/{id}", config.updateData(ApiData(map!!))).save()
        }

        given().get(URI.create("http://localhost:$localPort/oas/petstore/v1/pets/2"))
                .then()
                .statusCode(200)
                .header("Extra-Header", equalTo("extra-value"))
                .body("id", equalTo(2))
    }
}