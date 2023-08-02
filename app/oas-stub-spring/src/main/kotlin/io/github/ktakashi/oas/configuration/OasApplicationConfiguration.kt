package io.github.ktakashi.oas.configuration

import io.github.ktakashi.oas.engine.apis.API_PATH_NAME_QUALIFIER
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OasApplicationConfiguration {
    @Bean(API_PATH_NAME_QUALIFIER)
    fun apiPathPrefix() = "/oas"
}
