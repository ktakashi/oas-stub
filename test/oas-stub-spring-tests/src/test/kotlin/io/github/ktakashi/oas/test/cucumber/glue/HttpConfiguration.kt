package io.github.ktakashi.oas.test.cucumber.glue

import io.github.ktakashi.oas.test.ktor.createHttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.security.KeyStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HttpConfiguration(@param:Autowired(required = false) val keyStore: KeyStore?) {
    @Bean
    fun httpClient() = OkHttp.createHttpClient(keyStore, "oas-stub", "password")
}