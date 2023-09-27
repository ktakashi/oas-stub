package io.github.ktakashi.oas.test

import io.github.ktakashi.oas.configuration.AutoOasEngineConfiguration
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

internal const val OAS_STUB_TEST_SERVICE_NAME = "oasStubTestService"

@AutoConfiguration(
        after = [AutoOasEngineConfiguration::class]
)
@Configuration
@EnableConfigurationProperties(OasStubTestProperties::class)
class AutoOasStubTestConfiguration(private val properties: OasStubTestProperties) {
    @Bean(OAS_STUB_TEST_SERVICE_NAME)
    @ConditionalOnMissingBean
    fun oasStubTestService(apiRegistrationService: ApiRegistrationService, apiObserver: ApiObserver) = OasStubTestService(properties, apiRegistrationService, apiObserver)
}
