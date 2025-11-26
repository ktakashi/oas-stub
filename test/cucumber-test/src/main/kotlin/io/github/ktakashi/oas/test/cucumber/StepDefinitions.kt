package io.github.ktakashi.oas.test.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.Before
import io.cucumber.java.ParameterType
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headers
import java.io.IOException
import java.net.URI
import java.util.function.Supplier
import kotlin.time.DurationUnit
import kotlin.time.toTimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper


data class TestContext(var applicationUrl: String,
                       var prefix: String,
                       var adminPrefix: String,
                       var apiDefinitionPath: String = "",
                       var apiName: String = "",
                       var headers: MutableList<Pair<String, String>> = mutableListOf(),
                       var response: HttpResponse? = null,
                       var responseTime: Long? = null) {
    val responses = mutableListOf<HttpResponse>()
}

fun interface TestContextSupplier: Supplier<TestContext>

@Suppress("UNCHECKED_CAST")
class StepDefinitions(private val persistentStorage: PersistentStorage,
                      private val sessionStorage: SessionStorage,
                      private val client: HttpClient,
                      private val testContextSupplier: TestContextSupplier) {

    private lateinit var testContext: TestContext
    private val jsonMapper = JsonMapper.builder().findAndAddModules().build()

    @Before
    fun setup() {
        testContext = testContextSupplier.get()
        sessionStorage.clearApiMetrics()
        //RestAssured.config = RestAssured.config().redirect(RedirectConfig().followRedirects(false))
    }

    @ParameterType("yes|no")
    fun yesNo(value: String) = when (value) {
        "yes" -> true
        "no" -> false
        else -> error("shouldn't happen")
    }

    @Given("this API definition {string}")
    fun `this API definition {string}`(path: String) {
        testContext.apiDefinitionPath = path
    }

    @Given("this static stub {string}")
    fun thisStaticStub(context: String) {
        testContext.apiName = context
    }

    @Given("these HTTP headers")
    fun `these HTTP headers`(table: DataTable) {
        table.asMaps(String::class.java, String::class.java).forEach { m ->
            testContext.headers.add(m["name"] as String to m["value"] as String)
        }
    }

    @Given("This is custom controller tests")
    fun `This is custom controller tests`() {
        testContext.prefix = ""
    }

    @When("I create {string} API definition")
    fun `I create {string} API definition`(context: String) {
        persistentStorage.deleteApiDefinition(context)
        createAPI(context, "classpath:${testContext.apiDefinitionPath}", ContentType.Text.Plain)
    }

    @When("I create {string} API definition with {string}")
    fun iCreateApiDefinitionWithContent(context: String, configurations: String) {
        createAPI(context, configurations, ContentType.Application.Json)
    }

    private fun createAPI(context: String, configurations: String, contentType: ContentType) {
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(testContext.adminPrefix).pathSegment(context)
                .build().toUri()
        val response = runBlocking {
            client.post(uri.toURL()) {
                this.contentType(contentType)
                setBody(maybeContent(configurations))
            }
        }

        testContext.apiName = context
        testContext.response = response

        assertEquals(HttpStatusCode.Created, response.status)
        val location = response.headers["Location"] ?: error("Response location not found")
        assertEquals("/${context}", location)
    }

    @When("I delete the API definition")
    fun `I delete {string} API definition`() {
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(testContext.adminPrefix).pathSegment(testContext.apiName)
                .build().toUri()
        testContext.response = runBlocking { client.delete(uri.toURL()) }
    }

    @And("I update API definition with {string} via {string} of content type {string}")
    fun `I update {string} API definition with {string} via {string}`(value: String, path: String, contentType: String) {
        val adminApi = URI.create(path)
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(testContext.adminPrefix)
                .pathSegment(testContext.apiName)
                .path(adminApi.path)
                .query(adminApi.query)
                .build().toUri()
        testContext.response = runBlocking {
            client.put(uri.toURL()) {
                this.contentType(ContentType.parse(contentType))
                setBody(maybeContent(value))
            }
        }
    }

    @And("I get API definition via {string}")
    fun iGetApiDefinitionVia(path: String) {
        val adminApi = URI.create(path)
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(testContext.adminPrefix)
                .pathSegment(testContext.apiName)
                .path(adminApi.path)
                .query(adminApi.query)
                .build().toUri()
        testContext.response = runBlocking { client.get(uri.toURL()) }
    }

    @And("I get metrics of {string}")
    fun `I get metrics via {string}`(context: String) {
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
            .path(testContext.adminPrefix)
            .pathSegment("metrics", context)
            .build()
            .toUri()
        testContext.response = runBlocking { client.get(uri.toURL()) }
    }

    @Then("I get records of {string}")
    fun `I get records via {string}`(context: String) {
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
            .path(testContext.adminPrefix)
            .pathSegment("records", context)
            .build()
            .toUri()
        testContext.response = runBlocking { client.get(uri.toURL()) }
    }

    @And("I update API {string} with {string} via {string} of content type {string}")
    fun `I update API {string} with {string} via {string}`(api: String, value: String, path: String, contentType: String) {
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(testContext.adminPrefix)
                .pathSegment(testContext.apiName, "configurations")
                .path(path)
                .queryParam("api", api)
                .build().toUri()
        testContext.response = runBlocking {
            client.put(uri.toURL()) {
                this.contentType(ContentType.parse(contentType))
                setBody(maybeContent(value))
            }
        }
    }

    @And("I update API {string} of {string} method with {string} via {string} of content type {string}")
    fun `I update API {string} of {string} method with {string} via {string}`(api: String, method: String, value: String, path: String, contentType: String) {
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
            .path(testContext.adminPrefix)
            .pathSegment(testContext.apiName, "configurations")
            .path(path)
            .queryParam("api", api)
            .queryParam("method", method)
            .build().toUri()
        testContext.response = runBlocking {
            client.put(uri.toURL()) {
                this.contentType(ContentType.parse(contentType))
                setBody(maybeContent(value))
            }
        }
    }

    @And("I delete API {string} via {string}")
    fun `I delete API {string} via {string}`(api: String, path: String) {
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(testContext.adminPrefix)
                .pathSegment(testContext.apiName, "configurations")
                .path(path)
                .queryParam("api", api)
                .build().toUri()
        testContext.response = runBlocking { client.delete(uri.toURL()) }
    }

    @Then("I delete all metrics")
    fun iDeleteAllMetrics() {
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
            .path(testContext.adminPrefix)
            .pathSegment("metrics")
            .build()
            .toUri()
        testContext.response = runBlocking { client.delete(uri.toURL()) }
    }

    @Then("I {string} to {string} with {string} as {string}")
    fun iRequestToPathWithContentAsContentType(method: String, path: String, content: String, contentType: String) {
        requestApi(path, contentType, content, method)
    }

    @Then("I {string} to {string} with {string} as {string} for {int} times in {int} batches")
    fun iRequestToPathWithContentAsContentTypeForNTimes(method: String, path: String, content: String, contentType: String, count: Int, batch: Int) {
        val responses = runBlocking {
            (1..count).map {
                async {
                    requestApiInner(path, contentType, content, method)
                }
            }.awaitAll()
        }
        testContext.responses.addAll(responses)
        testContext.response = testContext.responses.firstOrNull()
    }


    @Then("[Protocol Error] I {string} to {string} with {string} as {string}")
    fun protocolError(method: String, path: String, content: String, contentType: String) {
        // Either client exception or illegal argument exception, but can't say which one for some weird reason
        assertThrows<Exception> { requestApi(path, contentType, content, method) }
    }

    @Then("[Connection Error] I {string} to {string} with {string} as {string}")
    fun connectionError(method: String, path: String, content: String, contentType: String) {
        // Either client exception or illegal argument exception, but can't say which one for some weird reason
        assertThrows<IOException> { requestApi(path, contentType, content, method) }
    }

    private fun requestApi(path: String, contentType: String, content: String, method: String) {
        val start = System.currentTimeMillis()
        testContext.response = runBlocking { requestApiInner(path, contentType, content, method) }
        testContext.responseTime = System.currentTimeMillis() - start
    }

    private suspend fun requestApiInner(path: String, contentType: String, content: String, method: String): HttpResponse {
        val p = URI.create(path)
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
            .path(testContext.prefix)
            .pathSegment(testContext.apiName)
            .path(p.path)
            .query(p.query)
            .build()
            .toUri()
        return client.request(uri.toURL()) {
            this.method = HttpMethod.parse(method)
            contentType(ContentType.parse(contentType))
            testContext.headers.forEach { (k, v) -> header(k, v) }
            maybeContent(content)?.let(::setBody)
        }
    }

    @Then("I get http status {int}")
    fun iGetHttpStatus(status: Int) {
        val result = testContext.response?.status ?: error("No response")
        assertEquals(HttpStatusCode.fromValue(status), result)
    }

    @Then("I save the response")
    fun iSaveTheResponse() {
        testContext.responses.add(testContext.response!!)
    }

    @Then("the responses are the same: {yesNo}")
    fun theResponsesAreNotTheSame(areSame: Boolean) {
        assert(testContext.response != null) { "Response is null" }
        assert(testContext.responses.lastOrNull() != null) { "The previous response must be saved" }
        runBlocking {
            val response = testContext.response?.bodyAsBytes()!!
            assertEquals(areSame, testContext.responses.last().bodyAsBytes().contentEquals(response))
        }
    }

    @Then("I get response header of {string} with {string}")
    fun iGetResponseHeaderOfWith(name: String, value: String) {
        val response = testContext.response
        assert(response != null)
        val headerValue = response!!.headers[name]
        when (value) {
            "<null>" -> assertTrue(headerValue == null)
            else -> assertEquals(value, headerValue!!)

        }
    }

    @Then("I get response JSON satisfies this {string}")
    fun iGetResponseJsonSatisfiesThis(condition: String) {
        if ("<null>" == condition) {
            val response = testContext.response ?: error("no response")
            val r = runBlocking { response.bodyAsBytes() }
            assertEquals(0, r.size)
        } else {
            val validatableResponse = testContext.response ?: error("No response")
            val node = runBlocking {
                val body = validatableResponse.bodyAsBytes()
                jsonMapper.readTree(body)
            }
            condition.split(';').forEach { cond ->
                val (path, value) = when (val hash = cond.indexOf('#')) {
                    -1 -> when (val eq = cond.indexOf('=')) {
                        -1 -> cond to { v -> v == null || v.isNull }
                        else -> cond.take(eq) to checkMarker(cond.substring(eq + 1))
                    }
                    else -> cond.take(hash) to resolveDSL(cond.substring(hash + 1))
                }
                val normalized = path.replace('.', '/').replace("[", "").replace("]", "")
                val v = node.at(if (normalized.isEmpty()) "" else "/$normalized")
                assertTrue(value(v), "$path = $v")
            }
        }
    }

    @Then("I get pattern of {string} as response")
    fun iGetPatternOfAsResponse(body: String) {
        val response = testContext.response ?: error("No response")
        val content = runBlocking { response.bodyAsText() }
        assertTrue(Regex(body).matches(content))
    }

    private fun resolveDSL(value: String): (JsonNode?) -> Boolean {
        val eq = value.indexOf('=')
        if (eq < 0) error("Invalid DSL: $value")
        val func = value.take(eq)
        val value = value.substring(eq + 1)
        return when (func) {
            "size()" -> { v -> v != null && v.isArray && v.size() == value.toInt() }
            else -> error("Unsupported DSL function: $func")
        }
    }

    val JsonNode?.isNullOrMissing: Boolean
        get() = this == null || this.isNull || this.isMissingNode

    private fun checkMarker(value: String): (JsonNode?) -> Boolean {
        return when (value) {
            "<null>" -> { v -> v.isNullOrMissing }
            "<uuid>" -> Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}").let {
                { v -> v != null && v.isString && it.matches(v.asString()) }
            }
            "true", "false" -> value.toBoolean().let {
                { v -> v != null && v.isBoolean && v.asBoolean() == it }
            }
            else -> { v -> v != null && v.asString() == value }
        }
    }


    @Then("I waited at least {long} {string}")
    fun `I waited at least {int} {string}`(duration: Long, unit: String) {
        val durationUnit = DurationUnit.valueOf(unit.uppercase())
        val response = testContext.response ?: error("No response")
        val time = response.responseTime.timestamp - response.requestTime.timestamp
        assertTrue(time >= durationUnit.toTimeUnit().toMillis(duration))
    }

    @Then("I waited at most {long} {string}")
    fun `I waited at most {int} {string}`(duration: Long, unit: String) {
        val durationUnit = DurationUnit.valueOf(unit.uppercase())
        val response = testContext.response ?: error("No response")
        val time = response.responseTime.timestamp - response.requestTime.timestamp
        assertTrue(time <= durationUnit.toTimeUnit().toMillis(duration))
    }

    @Then("Reading response took at least {long} {string}")
    fun `Reading response took at least {long} {string}`(duration: Long, unit: String) {
        val durationUnit = DurationUnit.valueOf(unit.uppercase())
        val responseTime = testContext.responseTime?: throw IllegalStateException("No response time")
        assertTrue(responseTime > durationUnit.toTimeUnit().toMillis(duration)) { "Reading entire response must take more than $duration$unit" }
    }
}

private fun maybeContent(content: String?) = content?.let {
    if (content.isNotEmpty()) {
        fromClassPathOrContent(content)
    } else null
}

private fun fromClassPathOrContent(content: String): ByteArray =
    if (content.startsWith("classpath:")) {
        readContent(content.substring("classpath:".length)).toByteArray()
    } else {
        content.toByteArray()
    }

private class Dummy
private fun readContent(path: String) = Dummy::class.java.getResourceAsStream(path)?.reader()?.buffered()?.use { it.readText() }
    ?: throw IllegalArgumentException("$path doesn't exist")
