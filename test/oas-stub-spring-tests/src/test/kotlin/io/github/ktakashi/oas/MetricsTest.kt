package io.github.ktakashi.oas

import com.fasterxml.jackson.databind.node.TextNode
import io.github.ktakashi.oas.test.AutoConfigureOasStubServer
import io.github.ktakashi.oas.test.OasStubServerPort
import io.github.ktakashi.oas.test.OasStubTestService
import io.github.ktakashi.oas.test.context
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import java.net.URI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.web.util.UriTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureOasStubServer(port = 0)
class MetricsTest(@OasStubServerPort private val port: Int,
                  @Autowired private val oasStubTestService: OasStubTestService) {
    @BeforeEach
    fun init() {
        oasStubTestService.context("petstore") {
            specification(ClassPathResource("/schema/v3/petstore-extended.yaml"))
            configuration("/v2/pets/{petId}") {
                data {
                    entry("/v2/pets/400", 400) {

                    }
                    entry("/v2/pets/404", 404) {

                    }
                }
            }
        }
    }

    @Test
    fun `test metrics endpoint`() {
        val ids = listOf(1, 2, 3, 4, 5, 400, 404)
        ids.forEach { id ->
            given().get(URI.create("http://localhost:$port/oas/petstore/v2/pets/$id"))
        }
        val metrics = oasStubTestService.getTestApiMetrics("petstore")
        assertEquals(5, metrics.byStatus(200).count())
        assertEquals(1, metrics.byStatus(400).count())
        ids.forEach { id ->
            assertEquals(1, metrics.byPath("/v2/pets/$id").count())
        }
        assertEquals(ids.size, metrics.byUriTemplate(UriTemplate("/v2/pets/{petId}")).count())
        assertEquals(1, metrics.byUriTemplate(UriTemplate("/v2/pets/{petId}")).byStatus(400).count())

        oasStubTestService.clearTestApiMetrics()
        assertTrue(oasStubTestService.getTestApiMetrics("petstore").count() == 0)
    }

    @Test
    fun `test records endpoint`() {
        RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())
        val ids = listOf(1, 2, 3, 4, 5, 400, 404)
        ids.forEach { id ->
            given()
                .header("Content-Type", "application/json")
                .body("""{"name":"name-$id","tag":"tag-$id"}""")
                .post(URI.create("http://localhost:$port/oas/petstore/v2/pets"))
        }
        val records = oasStubTestService.getTestApiRecords("petstore")
        assertEquals(ids.size, records.byStatus(200).count())
        assertEquals(ids.size, records.byMethod("POST").count())
        assertEquals(0, records.byMethod("GET").count())
        assertEquals(ids.size, records.byPath("/v2/pets").count())
        assertEquals(ids.size, records.byUriTemplate(UriTemplate("/v2/pets")).count())

        assertEquals(ids.size, records.byRequestHeader("Content-Type").count())
        assertEquals(ids.size, records.byRequestHeader("Content-Type", "application/json").count())
        assertEquals(0, records.byRequestHeader("Content-Type") { it?.contains("application/json") != true }.count())
        assertEquals(ids.size, records.byRequestBody().count())
        assertEquals(1, records.byRequestBody("""{"name":"name-1","tag":"tag-1"}""".toByteArray()).count())
        assertEquals(7, records.byRequestJson("/name").count())
        assertEquals(1, records.byRequestJson("/name", TextNode.valueOf("name-1")).count())


        assertEquals(ids.size, records.byResponseHeader("Content-Type").count())
        assertEquals(ids.size, records.byResponseHeader("Content-Type", "application/json").count())
        assertEquals(0, records.byResponseHeader("Content-Type") { it?.contains("application/json") != true }.count())
        assertEquals(ids.size, records.byResponseBody().count())
        assertEquals(7, records.byResponseBody("""{"name":"string","tag":"string","id":0}""".toByteArray()).count())
        assertEquals(7, records.byResponseJson("/name").count())
        assertEquals(7, records.byResponseJson("/name", TextNode.valueOf("string")).count())
    }
}