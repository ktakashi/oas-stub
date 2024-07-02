package io.github.ktakashi.oas

import io.github.ktakashi.oas.test.AutoConfigureOasStubServer
import io.github.ktakashi.oas.test.OasStubServerPort
import io.github.ktakashi.oas.test.OasStubTestService
import io.github.ktakashi.oas.test.context
import io.restassured.RestAssured.given
import java.net.URI
import org.junit.jupiter.api.Assertions.assertEquals
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
            specification(ClassPathResource("/schema/v3/petstore.yaml"))
            configuration("/v1/pets/{petId}") {
                data {
                    entry("/v1/pets/400", 400) {

                    }
                    entry("/v1/pets/404", 404) {

                    }
                }
            }
        }
    }

    @Test
    fun `test metrics endpoint`() {
        val ids = listOf(1, 2, 3, 4, 5, 400, 404)
        ids.forEach { id ->
            given().get(URI.create("http://localhost:$port/oas/petstore/v1/pets/$id"))
        }
        assertEquals(5, oasStubTestService.getTestApiMetrics("petstore").byStatus(200).count())
        assertEquals(1, oasStubTestService.getTestApiMetrics("petstore").byStatus(400).count())
        ids.forEach { id ->
            assertEquals(1, oasStubTestService.getTestApiMetrics("petstore").byPath("/v1/pets/$id").count())
        }
        assertEquals(ids.size, oasStubTestService.getTestApiMetrics("petstore").byUriTemplate(UriTemplate("/v1/pets/{petId}")).count())
        assertEquals(1, oasStubTestService.getTestApiMetrics("petstore").byUriTemplate(UriTemplate("/v1/pets/{petId}")).byStatus(400).count())
    }
}