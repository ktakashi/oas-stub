package io.github.ktakashi.oas.configuration

import io.github.ktakashi.oas.annotations.Admin
import io.github.ktakashi.oas.engine.apis.API_PATH_NAME_QUALIFIER
import io.github.ktakashi.oas.engine.apis.ApiDelayService
import io.github.ktakashi.oas.engine.apis.ApiExecutionService
import io.github.ktakashi.oas.servlets.OasDispatchServlet
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class OasApplicationConfiguration(private val oasApplicationServletProperties: OasApplicationServletProperties): WebMvcConfigurer {
    @Bean(API_PATH_NAME_QUALIFIER)
    fun apiPathPrefix() = oasApplicationServletProperties.prefix

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        super.configurePathMatch(configurer)
        configurer.addPathPrefix(oasApplicationServletProperties.adminPrefix,
                HandlerTypePredicate.forAnnotation(Admin::class.java))
    }
}

@Configuration
class OasServletConfiguration(private val oasApplicationServletProperties: OasApplicationServletProperties,
                              private val apiExecutionService: ApiExecutionService,
                              private val apiDelayService: ApiDelayService) {
    @Bean
    fun servletBean() = ServletRegistrationBean(OasDispatchServlet(apiExecutionService, apiDelayService), "${oasApplicationServletProperties.prefix}/*")
            .also { registration ->
                registration.setLoadOnStartup(1)
                registration.setAsyncSupported(true)
            }
}

@Component
@ConfigurationProperties(prefix = "oas.servlet")
data class OasApplicationServletProperties(val prefix: String = "/oas",
                                           val adminPrefix: String = "/__admin")
