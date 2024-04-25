package io.github.ktakashi.oas.reactive.configuration

import io.github.ktakashi.oas.configuration.AutoOasWebCoreConfiguration
import io.github.ktakashi.oas.configuration.OasApplicationServletProperties
import io.github.ktakashi.oas.engine.apis.ApiDelayService
import io.github.ktakashi.oas.engine.apis.ApiExecutionService
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.web.reactive.OasStubApiHandler
import io.github.ktakashi.oas.web.reactive.RouterFunctionBuilder
import io.github.ktakashi.oas.web.reactive.RouterFunctionFactory
import io.github.ktakashi.oas.web.reactive.rests.ApiRouterFunctionBuilder
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration(after = [AutoOasWebCoreConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@Configuration
class AutoOasReactiveConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun oasStubApiHandler(apiExecutionService: ApiExecutionService,
                          apiDelayService: ApiDelayService,
                          apiObserver: ApiObserver): OasStubApiHandler
            = OasStubApiHandler(apiExecutionService, apiDelayService, apiObserver)

    @Bean
    fun apiRouterFunctionBuilder(properties: OasApplicationServletProperties,
                                 apiRegistrationService: ApiRegistrationService): RouterFunctionBuilder =
        ApiRouterFunctionBuilder(properties.adminPrefix, apiRegistrationService)

    @Bean
    fun oasStubRouterFunction(properties: OasApplicationServletProperties,
                              apiHandler: OasStubApiHandler,
                              functionBuilders: Set<RouterFunctionBuilder>) =
        RouterFunctionFactory(apiHandler).buildRouterFunction(properties.prefix, functionBuilders)
}