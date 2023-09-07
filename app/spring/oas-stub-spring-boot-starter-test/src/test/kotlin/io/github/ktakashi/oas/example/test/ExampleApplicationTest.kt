package io.github.ktakashi.oas.example.test

import io.restassured.RestAssured.given
import java.net.URI
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
class ExampleApplicationTest(@Value("\${local.server.port}") private val localPort: Int) {
    @Test
    fun check() {
        given().get(URI.create("http://localhost:$localPort/oas/petstore/v1/pets/1"))
                .then()
                .statusCode(200)
                .body("id", equalTo(1))

        given().get(URI.create("http://localhost:$localPort/oas/petstore/v1/pets/2"))
                .then()
                .statusCode(404)
                .body("message", equalTo("No pet found"))
    }
}