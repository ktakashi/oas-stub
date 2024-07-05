package io.github.ktakashi.oas.example.test

import io.github.ktakashi.oas.model.ApiData
import io.github.ktakashi.oas.model.ApiFixedDelay
import io.github.ktakashi.oas.model.ApiHeaders
import io.github.ktakashi.oas.model.ApiHttpError
import io.github.ktakashi.oas.model.ApiLatency
import io.github.ktakashi.oas.model.ApiOptions
import io.github.ktakashi.oas.model.ApiProtocolFailure
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.model.PluginType
import io.github.ktakashi.oas.test.AutoConfigureOasStubServer
import io.github.ktakashi.oas.test.OasStubTestPlugin
import io.github.ktakashi.oas.test.OasStubTestProperties
import io.github.ktakashi.oas.test.OasStubTestResources
import io.github.ktakashi.oas.test.OasStubTestService
import io.github.ktakashi.oas.test.context
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import java.net.URI
import kotlin.time.DurationUnit
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureOasStubServer(port = 0, stubConfigurations = ["classpath:/stub-configuration.json"])
class ExampleApplicationTest(@Value("\${${OasStubTestProperties.OAS_STUB_SERVER_PROPERTY_PREFIX}.port}") private val localPort: Int,
                             @Autowired private val oasStubTestService: OasStubTestService) {
    @Test
    fun testPetstore() {
        val name = "petstore"
        check(name)

        val context = oasStubTestService.getTestApiContext(name)
            .updateHeaders(ApiHeaders(response = sortedMapOf("Extra-Header" to listOf("extra-value"))))
        context.getApiConfiguration("/v1/pets/{id}").ifPresent { config ->
            val value = OasStubTestResources.DefaultResponseModel(status = 200, response = """{"id": 2,"name": "Pochi","tag": "dog"}""")
            val map = config.data?.asMap()?.toMutableMap()
            map?.put("/v1/pets/2", value)
            context.updateApi("/v1/pets/{id}", config.updateData(ApiData(map!!))).save()
        }

        given().get(URI.create("http://localhost:$localPort/oas/$name/v1/pets/2"))
            .then()
            .statusCode(200)
            .header("Extra-Header", equalTo("extra-value"))
            .body("id", equalTo(2))
    }

    @Test
    fun testPetstoreStatic() {
        check("petstore-static")
    }

    @Test
    fun testDsl() {
        oasStubTestService.context("dsl-test") {
            headers {
                request {
                    header("Stub-Request-Header", "ok")
                }
                response {
                    header("Stub-Response-Header", "ok")
                }
            }
            options {
                shouldValidate(true)
                shouldMonitor(true)
                shouldRecord(true)
                latency {
                    interval(1)
                    unit(DurationUnit.NANOSECONDS)
                }
                failure {
                    protocol()
                }
            }
            delay {
                noDelay()
            }
            configuration("/api0") {
                headers {
                    request {
                        header("API0-Request-Header", "ok")
                    }
                    response {
                        header("API0-Response-Header", "ok")
                    }
                }
                delay {
                    fixed(1)
                }
                options {
                    failure {
                        status(500)
                    }
                }
                defaultPlugin()
            }
        }
        val context = oasStubTestService.getTestApiContext("dsl-test")
        val definitions = context.apiDefinitions
        assertNotNull(definitions.headers)
        assertEquals(setOf("Stub-Request-Header"), definitions.headers?.request?.keys)
        assertEquals(setOf("Stub-Response-Header"), definitions.headers?.response?.keys)
        assertNotNull(definitions.options)
        assertEquals(ApiOptions(shouldValidate = true,
            latency = ApiLatency(1, DurationUnit.NANOSECONDS),
            failure = ApiProtocolFailure,
            shouldMonitor = true,
            shouldRecord = true
        ), definitions.options)
        assertNull(definitions.delay)
        assertNotNull(definitions.configurations)
        val configuration = definitions.configurations!!["/api0"]
        assertNotNull(configuration)
        assertNotNull(configuration?.headers)
        assertEquals(setOf("API0-Request-Header"), configuration?.headers?.request?.keys)
        assertEquals(setOf("API0-Response-Header"), configuration?.headers?.response?.keys)
        assertEquals(ApiFixedDelay(1 ), configuration?.delay)
        assertEquals(ApiOptions(failure = ApiHttpError(500)), configuration?.options)
        assertEquals(OasStubTestPlugin().toPluginDefinition(), configuration?.plugin)
    }

    fun check(name: String) {
        RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())

        given().get(URI.create("http://localhost:$localPort/oas/$name/v1/pets/1"))
                .then()
                .statusCode(200)
                .body("id", equalTo(1))
        assertEquals(1, oasStubTestService.getTestApiMetrics(name).byPath("/v1/pets/1").count())

        given().get(URI.create("http://localhost:$localPort/oas/$name/v1/pets/2"))
                .then()
                .statusCode(404)
                .body("message", equalTo("No pet found"))
        assertEquals(1, oasStubTestService.getTestApiMetrics(name).byStatus(200).count())
        assertEquals(1, oasStubTestService.getTestApiMetrics(name).byStatus(404).count())
        assertEquals(2, oasStubTestService.getTestApiMetrics(name).filter { m -> m.httpMethod == "GET"}.count())
    }
}