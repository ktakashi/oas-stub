package io.github.ktakashi.oas.ktor.web

import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.test.mock.declareMock
import org.mockito.BDDMockito

val testModule = module {
    single<ApiRegistrationService> {
        declareMock {
            BDDMockito.given(getAllNames()).will { setOf("test") }
        }
    }
}

fun Application.module() {
    install(Koin) {
        modules(testModule)
    }
    install(ContentNegotiation) {
        json()
    }
    routing {
        apiInfoRouting()
    }
}