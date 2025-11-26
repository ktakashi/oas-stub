package io.github.ktakashi.oas

import io.github.ktakashi.oas.server.OasStubServer
import io.github.ktakashi.oas.server.api.OasStubApiByHeaderForwardingResolver
import io.github.ktakashi.oas.server.options.OasStubOptions
import io.github.ktakashi.oas.test.ktor.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ArrayNode

private const val FORWARDING_HEADER = "X-Oas-Stub-Application"

class PluginTest {
    companion object {
        lateinit var server: OasStubServer
        lateinit var client: HttpClient
        private val jsonMapper = JsonMapper.builder().findAndAddModules().build()
        @JvmStatic
        @BeforeAll
        fun setup() {
            val options = OasStubOptions.builder()
                .serverOptions()
                .port(0)
                .parent()
                .stubOptions()
                .forwardingPath("/")
                .addForwardingResolver(OasStubApiByHeaderForwardingResolver(FORWARDING_HEADER))
                .enableRecords(true)
                .addStaticConfiguration("classpath:/static-config.yaml")
                .parent()
                .build()
            server = OasStubServer(options)
            server.start()
            client = CIO.createHttpClient()
        }
    }

    @BeforeEach
    fun init() {
        server.resetMetrics()
        server.resetRecords()
    }

    @Test
    fun staticPluginTest() {
        val r = runBlocking { client.get("http://localhost:${server.port()}/oas/test-api-static/examples") }
        assertEquals(HttpStatusCode.NotFound, r.status)
        assertEquals("12345", r.headers["X-Example-Id"])
        assertEquals(3, r.headers.getAll("X-Example-Values")?.count())
        assertTrue(setOf("a", "b", "c").containsAll(r.headers.getAll("X-Example-Values") ?: emptyList()))

        val response = runBlocking { client.get("http://localhost:${server.port()}/__admin/metrics/test-api-static") }
        assertEquals(HttpStatusCode.OK, response.status)
        val content = jsonMapper.readTree(runBlocking { response.bodyAsBytes() })
        val metrics = content["metrics"]
        val examples = metrics["/examples"]
        assertTrue(examples is ArrayNode)
        assertEquals(1, (examples as ArrayNode).size())
    }

    @Test
    fun listApiDataTest() {
        val response = runBlocking { client.get("http://localhost:${server.port()}/oas/test-api-static/examples/objects") }
        assertEquals(HttpStatusCode.OK, response.status)
        val content = jsonMapper.readTree(runBlocking { response.bodyAsBytes() })
        assertTrue(content is ArrayNode)
        val arrayNode = content as ArrayNode
        assertEquals(4, arrayNode.size())
        assertEquals("oas", arrayNode[0].asString())
        assertEquals("stub", arrayNode[1].asString())
        assertEquals("is", arrayNode[2].asString())
        assertEquals("great", arrayNode[3].asString())

        val response2 = runBlocking { client.get("http://localhost:${server.port()}/__admin/metrics/test-api-static") }
        assertEquals(HttpStatusCode.OK, response2.status)

        val content2 = jsonMapper.readTree(runBlocking { response2.bodyAsBytes() })
        val metrics = content2["metrics"]
        val examples = metrics["/examples/objects"]
        assertTrue(examples is ArrayNode)
        assertEquals(1, (examples as ArrayNode).size())
    }

    @Test
    fun forwardingTest() {
        val response = runBlocking {
            client.get("http://localhost:${server.port()}/examples/objects") {
                header(FORWARDING_HEADER, "test-api-static")
            }
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val content = jsonMapper.readTree(runBlocking { response.bodyAsBytes() })
        assertTrue(content is ArrayNode)
        val arrayNode = content as ArrayNode
        assertEquals(4, arrayNode.size())
        assertEquals("oas", arrayNode[0].asString())
        assertEquals("stub", arrayNode[1].asString())
        assertEquals("is", arrayNode[2].asString())
        assertEquals("great", arrayNode[3].asString())
    }
}