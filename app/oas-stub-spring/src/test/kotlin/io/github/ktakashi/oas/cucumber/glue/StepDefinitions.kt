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
import io.github.ktakashi.oas.models.CreateApiRequest
import io.github.ktakashi.oas.readContent
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import io.restassured.http.Header
import io.restassured.http.Headers
import java.net.URI
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.util.UriComponentsBuilder

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
@EnableAutoConfiguration
class StepDefinitions(@Value("\${local.server.port}") private val localPort: Int,
                      private val oasApplicationServletProperties: OasApplicationServletProperties) {
    private lateinit var testContext: TestContext

    companion object {
        init {
            RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())
        }
    }

    @Before
    fun setup() {
        testContext = TestContext("http://localhost:$localPort")
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

    @When("I create {string} API definition")
    fun `I create {string} API definition`(context: String) {
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(oasApplicationServletProperties.adminPrefix).pathSegment(context)
                .build().toUri()
        val response = given().contentType(ContentType.TEXT)
                .body(readContent(testContext.apiDefinitionPath))
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
        println(testContext.response?.body?.asString())
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

    @And("I {string} to {string} with {string} as {string}")
    fun `I {string} to {string} with {string} as {string}`(method: String, path: String, content: String, contentType: String) {
        val p = URI.create(path)
        val uri = UriComponentsBuilder.fromUriString(testContext.applicationUrl)
                .path(oasApplicationServletProperties.prefix)
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
            "POST" -> spec.apply { body?.let { spec.body(body)} }.post(uri)
            "PUT" -> spec.apply { body?.let { spec.body(body)} }.put(uri)
            "DELETE" -> spec.delete(uri)
            "PATCH" -> spec.apply { body?.let { spec.body(body)} }.patch(uri)
            "OPTIONS" -> spec.options(uri)
            else -> throw IllegalArgumentException("Not supported (yet?)")
        }
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
            val (path, value) = condition.lastIndexOf('=').let { if (it < 0) condition to "" else condition.substring(0, it) to condition.substring(it + 1) }
            testContext.response?.then()?.body(path, equalTo(value)) ?: throw IllegalStateException("No response")
        }
    }
}
