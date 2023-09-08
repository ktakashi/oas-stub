package io.github.ktakashi.oas.cucumber.glue

import io.cucumber.datatable.DataTable
import io.cucumber.java.Before
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.cucumber.spring.CucumberContextConfiguration
import io.github.ktakashi.oas.configuration.OasApplicationServletProperties
import io.github.ktakashi.oas.cucumber.context.TestContext
import io.github.ktakashi.oas.maybeContent
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.storages.apis.conditions.OAS_STUB_STORAGE_TYPE_PERSISTENT
import io.github.ktakashi.oas.storages.apis.conditions.OAS_STUB_STORAGE_TYPE_SESSION
import io.github.ktakashi.oas.storages.hazelcast.HazelcastPersistentStorage
import io.github.ktakashi.oas.storages.hazelcast.HazelcastSessionStorage
import io.github.ktakashi.oas.storages.inmemory.InMemoryPersistentStorage
import io.github.ktakashi.oas.storages.inmemory.InMemorySessionStorage
import io.github.ktakashi.oas.storages.mongodb.MongodbPersistentStorage
import io.github.ktakashi.oas.storages.mongodb.MongodbSessionStorage
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import io.restassured.http.Header
import io.restassured.http.Headers
import java.net.URI
import kotlin.time.DurationUnit
import kotlin.time.toTimeUnit
import org.apache.http.client.ClientProtocolException
import org.hamcrest.Matcher
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.lessThan
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.web.util.UriComponentsBuilder

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
@EnableAutoConfiguration
@DirtiesContext
class StepDefinitions(@Value("\${local.server.port}") private val localPort: Int,
                      private val persistentStorage: PersistentStorage,
                      private val sessionStorage: SessionStorage,
                      private val oasApplicationServletProperties: OasApplicationServletProperties,
                      @Value("\${${OAS_STUB_STORAGE_TYPE_PERSISTENT}:inmemory}") private val persistentStorageType: String,
                      @Value("\${${OAS_STUB_STORAGE_TYPE_SESSION}:inmemory}") private val sessionStorageType: String) {
    private lateinit var testContext: TestContext

    companion object {
        init {
            RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())
        }
    }

    @Before
    fun setup() {
        testContext = TestContext("http://localhost:$localPort", oasApplicationServletProperties.prefix, )
    }

    @Given("this API definition {string}")
    fun `this API definition {string}`(path: String) {
        testContext.apiDefinitionPath = path
    }

    @Given("these HTTP headers")
    fun `these HTTP headers`(table: DataTable) {
        table.asMaps(String::class.java, String::class.java).forEach { m ->
            testContext.headers.add(Header(m["name"] as String, m["value"] as String))
        }
    }

    @Given("This is custom controller tests")
    fun `This is custom controller tests`() {
        testContext.prefix = ""
    }

    @When("I create {string} API definition")
    fun `I create {string} API definition`(context: String) {
        persistentStorage.deleteApiDefinition(context)
        createAPI(context, "classpath:${testContext.apiDefinitionPath}", ContentType.TEXT)
    }

    @When("I create {string} API definition with {string}")
    fun iCreateApiDefinitionWithContent(context: String, configurations: String) {
        createAPI(context, configurations, ContentType.JSON)
    }

    private fun createAPI(context: String, configurations: String, contentType: ContentType) {
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(oasApplicationServletProperties.adminPrefix).pathSegment(context)
                .build().toUri()
        val response = given().contentType(contentType)
                .body(maybeContent(configurations))
                .post(uri)
        testContext.apiName = context
        testContext.response = response
        response.then().statusCode(201)
                .and().header("Location", "/${context}")
    }

    @When("I delete the API definition")
    fun `I delete {string} API definition`() {
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(oasApplicationServletProperties.adminPrefix).pathSegment(testContext.apiName)
                .build().toUri()
        testContext.response = given().delete(uri)
    }

    @And("I update API definition with {string} via {string} of content type {string}")
    fun `I update {string} API definition with {string} via {string}`(value: String, path: String, contentType: String) {
        val adminApi = URI.create(path)
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(oasApplicationServletProperties.adminPrefix)
                .pathSegment(testContext.apiName)
                .path(adminApi.path)
                .query(adminApi.query)
                .build().toUri()
        testContext.response = given().contentType(contentType)
                .body(maybeContent(value))
                .put(uri)
    }

    @And("I get API definition via {string}")
    fun `I get API definition via {string}`(path: String) {
        val adminApi = URI.create(path)
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(oasApplicationServletProperties.adminPrefix)
                .pathSegment(testContext.apiName)
                .path(adminApi.path)
                .query(adminApi.query)
                .build().toUri()
        testContext.response = given().get(uri)
    }

    @And("I update API {string} with {string} via {string} of content type {string}")
    fun `I update API {string} with {string} via {string}`(api: String, value: String, path: String, contentType: String) {
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(oasApplicationServletProperties.adminPrefix)
                .pathSegment(testContext.apiName, "configurations")
                .path(path)
                .queryParam("api", api)
                .build().toUri()
        testContext.response = given().contentType(contentType)
                .body(maybeContent(value))
                .put(uri)
    }

    @And("I delete API {string} via {string}")
    fun `I delete API {string} via {string}`(api: String, path: String) {
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(oasApplicationServletProperties.adminPrefix)
                .pathSegment(testContext.apiName, "configurations")
                .path(path)
                .queryParam("api", api)
                .build().toUri()
        testContext.response = given().delete(uri)
    }

    @Then("I {string} to {string} with {string} as {string}")
    fun `I {string} to {string} with {string} as {string}`(method: String, path: String, content: String, contentType: String) {
        requestApi(path, contentType, content, method)
    }


    @Then("[Protocol Error] I {string} to {string} with {string} as {string}")
    fun `Protocol Error I {string} to {string} with {string} as {string}`(method: String, path: String, content: String, contentType: String) {
        assertThrows<ClientProtocolException> { requestApi(path, contentType, content, method) }
    }

    private fun requestApi(path: String, contentType: String, content: String, method: String) {
        val start = System.currentTimeMillis()
        val p = URI.create(path)
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(testContext.prefix)
                .pathSegment(testContext.apiName)
                .path(p.path)
                .query(p.query)
                .build()
                .toUri()
        val spec = given().also {
            if (contentType.isNotBlank()) {
                it.contentType(contentType)
            }
        }.headers(Headers(testContext.headers))

        val body = maybeContent(content)
        testContext.response = when (method.uppercase()) {
            "GET" -> spec.get(uri)
            "POST" -> spec.apply { body?.let { spec.body(body) } }.post(uri)
            "PUT" -> spec.apply { body?.let { spec.body(body) } }.put(uri)
            "DELETE" -> spec.delete(uri)
            "PATCH" -> spec.apply { body?.let { spec.body(body) } }.patch(uri)
            "OPTIONS" -> spec.options(uri)
            else -> throw IllegalArgumentException("Not supported (yet?)")
        }
        testContext.responseTime = System.currentTimeMillis() - start
    }

    @Then("I get http status {int}")
    fun `I get this {int}`(status: Int) {
        testContext.response?.then()?.statusCode(status) ?: throw IllegalStateException("No response")
    }

    @Then("I get response header of {string} with {string}")
    fun `I get response header of {string} with {string}`(name: String, value: String) {
        val matcher = if ("<null>" != value) equalTo(value) else equalTo(null)
        testContext.response?.then()?.header(name, matcher) ?: throw IllegalStateException("no response")
    }

    @Then("I get response JSON satisfies this {string}")
    fun `I get response JSON satisfies this {string}`(condition: String) {
        if ("<null>" == condition) {
            val body = testContext.response?.body() ?: throw IllegalStateException("no response")
            val r = body.asByteArray()
            assertEquals(0, r.size)
        } else {
            val validatableResponse = testContext.response?.then()?: throw IllegalStateException("No response")
            condition.split(';').forEach { cond ->
                val (path, matcher) = cond.lastIndexOf('=').let {
                    if (it < 0) cond to equalTo(null)
                    else cond.substring(0, it) to checkMarker(cond.substring(it + 1))
                }
                validatableResponse.body(path, matcher)
            }
        }
    }

    private fun checkMarker(value: String): Matcher<Any?> {
        return when (value) {
            "<null>" -> equalTo(null)
            "<uuid>" -> matchesPattern("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") as Matcher<Any?>
            "true", "false" -> equalTo(value.toBoolean())
            else -> equalTo(value)
        }
    }


    @Then("I waited at least {long} {string}")
    fun `I waited at least {int} {string}`(duration: Long, unit: String) {
        val durationUnit = DurationUnit.valueOf(unit.uppercase())
        testContext.response?.then()?.time(greaterThan(duration), durationUnit.toTimeUnit()) ?: throw IllegalStateException("No response")
    }

    @Then("I waited at most {long} {string}")
    fun `I waited at most {int} {string}`(duration: Long, unit: String) {
        val durationUnit = DurationUnit.valueOf(unit.uppercase())
        testContext.response?.then()?.time(lessThan(duration), durationUnit.toTimeUnit()) ?: throw IllegalStateException("No response")
    }

    @Then("Reading response took at least {long} {string}")
    fun `Reading response took at least {long} {string}`(duration: Long, unit: String) {
        val durationUnit = DurationUnit.valueOf(unit.uppercase())
        val responseTime = testContext.responseTime?: throw IllegalStateException("No response time")
        assertTrue(responseTime > durationUnit.toTimeUnit().toMillis(duration)) { "Reading entire response must take more than $duration$unit" }
    }

    @Then("I have proper storages")
    fun `I have proper storages`() {
        println("$persistentStorageType = $persistentStorage")
        when (persistentStorageType) {
            "hazelcast" -> assertTrue(persistentStorage is HazelcastPersistentStorage)
            "mongodb" -> assertTrue(persistentStorage is MongodbPersistentStorage)
            else -> assertTrue(persistentStorage is InMemoryPersistentStorage)
        }

        println("$sessionStorageType = $sessionStorage")
        when (sessionStorageType) {
            "hazelcast" -> assertTrue(sessionStorage is HazelcastSessionStorage)
            "mongodb" -> assertTrue(sessionStorage is MongodbSessionStorage)
            else -> assertTrue(sessionStorage is InMemorySessionStorage)
        }
    }
}
