package io.github.ktakashi.oas.ktor.web

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.assertContains
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.KoinTest
import org.koin.test.junit5.mock.MockProviderExtension
import org.mockito.Mockito


class AdminRoutesTest: KoinTest {
    @JvmField
    @RegisterExtension
    val mockProvider = MockProviderExtension.create { klass -> Mockito.mock(klass.java) }
    @Test
    fun testApiInfoRoute() = testApplication {
        val client = createClient {
            install(Logging)
            install(ContentNegotiation) {
                json()
            }
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        val names = response.body<Set<String>>()
        assertContains(names, "test")
    }
}