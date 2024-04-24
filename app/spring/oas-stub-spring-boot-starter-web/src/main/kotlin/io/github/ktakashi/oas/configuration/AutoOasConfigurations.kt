package io.github.ktakashi.oas.configuration

import io.github.ktakashi.oas.engine.apis.ApiDelayService
import io.github.ktakashi.oas.engine.apis.ApiExecutionService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.jersey.OasStubResourceConfig
import io.github.ktakashi.oas.web.annotations.Admin
import io.github.ktakashi.oas.web.aspects.DelayableAspect
import io.github.ktakashi.oas.web.services.ExecutorProvider
import io.github.ktakashi.oas.web.servlets.OasDispatchServlet
import org.glassfish.jersey.server.ResourceConfig
import org.springdoc.core.properties.SwaggerUiConfigParameters
import org.springdoc.core.properties.SwaggerUiConfigProperties
import org.springdoc.webmvc.ui.SwaggerConfig
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.DefaultJerseyApplicationPath
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@AutoConfiguration(
        before = [JerseyAutoConfiguration::class, SwaggerConfig::class],
        after = [AutoOasWebCoreConfiguration::class]
)
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(OasApplicationServletProperties::class)
class AutoOasWebConfiguration(private val oasApplicationServletProperties: OasApplicationServletProperties): WebMvcConfigurer {

    @Bean
    @ConditionalOnMissingBean
    fun jerseyApplicationPath(resourceConfig: ResourceConfig) = DefaultJerseyApplicationPath(oasApplicationServletProperties.adminPrefix, resourceConfig)

    @Bean
    @ConditionalOnMissingBean
    fun resourceConfig() = OasStubResourceConfig(oasApplicationServletProperties.adminPrefix)

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        super.configurePathMatch(configurer)
        // in case custom @Admin controller (spring, I think) is defined
        configurer.addPathPrefix(oasApplicationServletProperties.adminPrefix,
                HandlerTypePredicate.forAnnotation(Admin::class.java))
    }

    @Bean
    @ConditionalOnMissingBean
    fun oasDispatchServlet(apiExecutionService: ApiExecutionService,
                           apiDelayService: ApiDelayService,
                           apiObserver: ApiObserver,
                           executorProvider: ExecutorProvider)
            = OasDispatchServlet(apiExecutionService, apiDelayService, apiObserver, executorProvider)

    @Bean
    @ConditionalOnMissingBean
    fun delayableAspect(apiDelayService: ApiDelayService, executorProvider: ExecutorProvider) = DelayableAspect(apiDelayService, executorProvider)

    @Bean
    fun servletBean(servlet: OasDispatchServlet) = ServletRegistrationBean(servlet, "${oasApplicationServletProperties.prefix}/*").also { registration ->
        registration.setLoadOnStartup(1)
        registration.setAsyncSupported(true)
    }

    // sort of abuse
    @Bean
    @ConditionalOnMissingBean
    fun swaggerUiConfigParameters(swaggerUiConfig: SwaggerUiConfigProperties): SwaggerUiConfigParameters {
        swaggerUiConfig.url = "${oasApplicationServletProperties.adminPrefix}/openapi.json"
        return SwaggerUiConfigParameters(swaggerUiConfig)
    }
}
