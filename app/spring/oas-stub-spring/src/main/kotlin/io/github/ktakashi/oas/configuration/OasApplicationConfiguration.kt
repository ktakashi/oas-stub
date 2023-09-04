package io.github.ktakashi.oas.configuration

import io.github.ktakashi.oas.engine.apis.API_PATH_NAME_QUALIFIER
import io.github.ktakashi.oas.web.annotations.Admin
import io.github.ktakashi.oas.web.rests.ContextConfigurationsController
import io.github.ktakashi.oas.web.rests.ContextController
import io.github.ktakashi.oas.web.rests.ContextDataController
import io.github.ktakashi.oas.web.rests.ContextDelayController
import io.github.ktakashi.oas.web.rests.ContextHeadersController
import io.github.ktakashi.oas.web.rests.ContextOptionsController
import io.github.ktakashi.oas.web.rests.DataConfigurationsController
import io.github.ktakashi.oas.web.rests.DelayConfigurationsController
import io.github.ktakashi.oas.web.rests.HeadersConfigurationsController
import io.github.ktakashi.oas.web.rests.OptionsConfigurationsController
import io.github.ktakashi.oas.web.rests.PluginConfigurationsController
import io.github.ktakashi.oas.web.servlets.OasDispatchServlet
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.server.ServerProperties
import org.springframework.boot.autoconfigure.web.servlet.DefaultJerseyApplicationPath
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class OasApplicationConfiguration(private val oasApplicationServletProperties: OasApplicationServletProperties): WebMvcConfigurer {

    @Bean(API_PATH_NAME_QUALIFIER)
    fun apiPathPrefix() = oasApplicationServletProperties.prefix

    @Bean
    fun jerseyApplicationPath(resourceConfig: ResourceConfig) = DefaultJerseyApplicationPath(oasApplicationServletProperties.adminPrefix, resourceConfig)

    @Bean
    fun resourceConfig() = ResourceConfig().apply {
        register(ContextController::class.java)
        register(ContextOptionsController::class.java)
        register(ContextConfigurationsController::class.java)
        register(ContextHeadersController::class.java)
        register(ContextDataController::class.java)
        register(ContextDelayController::class.java)

        register(PluginConfigurationsController::class.java)
        register(HeadersConfigurationsController::class.java)
        register(OptionsConfigurationsController::class.java)
        register(DataConfigurationsController::class.java)
        register(DelayConfigurationsController::class.java)

        property(ServerProperties.LOCATION_HEADER_RELATIVE_URI_RESOLUTION_DISABLED, true)
    }

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        super.configurePathMatch(configurer)
        // in case custom @Admin controller (spring, I think) is defined
        configurer.addPathPrefix(oasApplicationServletProperties.adminPrefix,
                HandlerTypePredicate.forAnnotation(Admin::class.java))
    }
}

@Configuration
class OasServletConfiguration(private val oasApplicationServletProperties: OasApplicationServletProperties,
                              private val servlet: OasDispatchServlet) {
    @Bean
    fun servletBean() = ServletRegistrationBean(servlet, "${oasApplicationServletProperties.prefix}/*")
            .also { registration ->
                registration.setLoadOnStartup(1)
                registration.setAsyncSupported(true)
            }
}

@ConfigurationProperties(prefix = "oas.servlet")
data class OasApplicationServletProperties(var prefix: String = "/oas",
                                           var adminPrefix: String = "/__admin")

@ConfigurationProperties(prefix = "oas.executors")
data class ExecutorsProperties(var parallelism: Int = Runtime.getRuntime().availableProcessors())
