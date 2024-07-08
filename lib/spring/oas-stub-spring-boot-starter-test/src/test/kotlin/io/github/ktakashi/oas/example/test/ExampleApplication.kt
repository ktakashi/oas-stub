package io.github.ktakashi.oas.example.test

import io.github.ktakashi.oas.server.api.OasStubApiByHeaderForwardingResolver
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

const val X_OAS_STUB_APPLICATION = "X-OAS-Stub-Application"

@SpringBootApplication
class ExampleApplication {
    @Bean
    fun byHeaderResolver() = OasStubApiByHeaderForwardingResolver(X_OAS_STUB_APPLICATION)
}