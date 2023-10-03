package io.github.ktakashi.oas.test.configurations

import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.test.OAS_STUB_TEST_SERVICE_BEAN_NAME
import io.github.ktakashi.oas.test.OasStubTestProperties
import io.github.ktakashi.oas.test.OasStubTestService
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
@EnableConfigurationProperties(OasStubTestProperties::class)
@EnableAutoConfiguration
class OasStubTestConfiguration(private val properties: OasStubTestProperties) {
    @Bean(OAS_STUB_TEST_SERVICE_BEAN_NAME)
    @ConditionalOnMissingBean
    fun oasStubTestService(apiRegistrationService: ApiRegistrationService, apiObserver: ApiObserver) = OasStubTestService(properties, apiRegistrationService, apiObserver)
}
