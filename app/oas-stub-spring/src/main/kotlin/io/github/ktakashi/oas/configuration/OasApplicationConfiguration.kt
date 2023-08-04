package io.github.ktakashi.oas.configuration

import io.github.ktakashi.oas.engine.apis.API_PATH_NAME_QUALIFIER
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
class OasApplicationConfiguration(private val oasApplicationServletProperties: OasApplicationServletProperties) {
    @Bean(API_PATH_NAME_QUALIFIER)
    fun apiPathPrefix() = oasApplicationServletProperties.prefix
}

@Component
@ConfigurationProperties(prefix = "oas.servlet")
data class OasApplicationServletProperties(val prefix: String = "/oas")
