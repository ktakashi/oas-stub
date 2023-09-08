package io.github.ktakashi.oas.configuration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.apis.API_PATH_NAME_QUALIFIER
import io.github.ktakashi.oas.engine.apis.ApiAnyDataPopulator
import io.github.ktakashi.oas.engine.apis.ApiContentDecider
import io.github.ktakashi.oas.engine.apis.ApiDataPopulator
import io.github.ktakashi.oas.engine.apis.ApiDataValidator
import io.github.ktakashi.oas.engine.apis.ApiDelayService
import io.github.ktakashi.oas.engine.apis.ApiExecutionService
import io.github.ktakashi.oas.engine.apis.ApiPathService
import io.github.ktakashi.oas.engine.apis.ApiRequestBodyValidator
import io.github.ktakashi.oas.engine.apis.ApiRequestParameterValidator
import io.github.ktakashi.oas.engine.apis.ApiRequestPathVariableValidator
import io.github.ktakashi.oas.engine.apis.ApiRequestSecurityValidator
import io.github.ktakashi.oas.engine.apis.ApiRequestValidator
import io.github.ktakashi.oas.engine.apis.ApiResultProvider
import io.github.ktakashi.oas.engine.apis.DefaultApiService
import io.github.ktakashi.oas.engine.parsers.ParsingService
import io.github.ktakashi.oas.engine.plugins.PluginCompiler
import io.github.ktakashi.oas.engine.plugins.PluginService
import io.github.ktakashi.oas.engine.plugins.groovy.GroovyPluginCompiler
import io.github.ktakashi.oas.engine.storages.StorageService
import io.github.ktakashi.oas.openapi.OAS_APPLICATION_PATH_CONFIG
import io.github.ktakashi.oas.services.DefaultExecutorProvider
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.storages.inmemory.configurations.AutoInMemoryPersistentStorageConfiguration
import io.github.ktakashi.oas.storages.inmemory.configurations.AutoInMemorySessionStorageConfiguration
import io.github.ktakashi.oas.web.annotations.Admin
import io.github.ktakashi.oas.web.aspects.DelayableAspect
import io.github.ktakashi.oas.web.rests.ApiController
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
import io.github.ktakashi.oas.web.services.ExecutorProvider
import io.github.ktakashi.oas.web.servlets.OasDispatchServlet
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.server.ServerProperties
import org.springdoc.core.properties.SwaggerUiConfigParameters
import org.springdoc.core.properties.SwaggerUiConfigProperties
import org.springdoc.webmvc.ui.SwaggerConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.DefaultJerseyApplicationPath
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


// *sigh*...
@AutoConfiguration(
        // At least these are needed, if others are also there, then that's also fine
        after = [AutoInMemoryPersistentStorageConfiguration::class, AutoInMemorySessionStorageConfiguration::class]
)
@Configuration
class AutoOasStorageConfiguration {
    // Because of this dependency, we can't use component scan
    // Though, spring boot official document says, don't mex with auto-configuration...
    @Bean
    @ConditionalOnMissingBean
    fun storageService(parsingService: ParsingService, persistentStorage: PersistentStorage, sessionStorage: SessionStorage) = StorageService(parsingService, persistentStorage, sessionStorage)

}

@AutoConfiguration(after = [AutoOasStorageConfiguration::class])
@Configuration
class AutoOasPluginConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun groovyPluginCompiler() = GroovyPluginCompiler()

    @Bean
    @ConditionalOnMissingBean
    fun pluginService(pluginCompilers: Set<PluginCompiler>, storageService: StorageService, objectMapper: ObjectMapper) = PluginService(pluginCompilers, storageService, objectMapper)
}

@AutoConfiguration
@Configuration
// These 2 packages are safe to scan... and keep them safe
@ComponentScan(value = ["io.github.ktakashi.oas.engine.validators", "io.github.ktakashi.oas.engine.apis.json"])
class AutoOasValidatorConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun apiRequestParameterValidator(validators: Set<ApiDataValidator<JsonNode>>) = ApiRequestParameterValidator(validators)

    @Bean
    @ConditionalOnMissingBean
    fun apiRequestBodyValidator(validators: Set<ApiDataValidator<JsonNode>>) = ApiRequestBodyValidator(validators)

    @Bean
    @ConditionalOnMissingBean
    fun apiRequestSecurityValidator() = ApiRequestSecurityValidator()

    @Bean
    @ConditionalOnMissingBean
    fun apiRequestPathVariableValidator(requestParameterValidator: ApiRequestParameterValidator, objectMapper: ObjectMapper)
            = ApiRequestPathVariableValidator(requestParameterValidator, objectMapper)
}

@AutoConfiguration(after = [AutoOasPluginConfiguration::class])
@Configuration
@EnableConfigurationProperties(OasApplicationServletProperties::class)
class AutoOasApiConfiguration(private val oasApplicationServletProperties: OasApplicationServletProperties) {

    @Bean(API_PATH_NAME_QUALIFIER)
    @ConditionalOnMissingBean
    fun apiPathPrefix() = oasApplicationServletProperties.prefix

    @Bean
    @ConditionalOnMissingBean
    fun apiContentDecider(validators: Set<ApiRequestValidator>, objectMapper: ObjectMapper) = ApiContentDecider(validators, objectMapper)

    @Bean
    @ConditionalOnMissingBean
    fun apiDelayService(storageService: StorageService) = ApiDelayService(storageService)

    @Bean
    @ConditionalOnMissingBean
    fun apiPathService(@Qualifier(API_PATH_NAME_QUALIFIER) prefix: String) = ApiPathService(prefix)

    @Bean
    @ConditionalOnMissingBean
    fun apiResultProvider(contentDecider: ApiContentDecider, populators: Set<ApiDataPopulator>, anyPopulators: Set<ApiAnyDataPopulator>)
            = ApiResultProvider(contentDecider, populators, anyPopulators)
}

@AutoConfiguration(after = [AutoOasApiConfiguration::class])
@Configuration
class AutoOasEngineConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun parsingService() = ParsingService()

    @Bean
    @ConditionalOnMissingBean
    fun apiService(storageService: StorageService,
                   apiPathService: ApiPathService,
                   apiRequestPathVariableValidator: ApiRequestPathVariableValidator,
                   apiResultProvider: ApiResultProvider,
                   pluginService: PluginService): ApiExecutionService
            = DefaultApiService(storageService, parsingService(), apiPathService, apiRequestPathVariableValidator, apiResultProvider, pluginService)
}

@AutoConfiguration(
        before = [JerseyAutoConfiguration::class, SwaggerConfig::class],
        after = [AutoOasEngineConfiguration::class]
)
@Configuration
@EnableConfigurationProperties(ExecutorsProperties::class)
class AutoOasWebConfiguration(private val oasApplicationServletProperties: OasApplicationServletProperties): WebMvcConfigurer {

    @Bean
    @ConditionalOnMissingBean
    fun executorProvider(executorsProperties: ExecutorsProperties): ExecutorProvider = DefaultExecutorProvider(executorsProperties)

    @Bean
    @ConditionalOnMissingBean
    fun jerseyApplicationPath(resourceConfig: ResourceConfig) = DefaultJerseyApplicationPath(oasApplicationServletProperties.adminPrefix, resourceConfig)

    @Bean
    @ConditionalOnMissingBean
    fun resourceConfig() = ResourceConfig().apply {
        register(ApiController::class.java)
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
        property(OAS_APPLICATION_PATH_CONFIG, oasApplicationServletProperties.adminPrefix)
        register(OpenApiResource::class.java)
    }

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        super.configurePathMatch(configurer)
        // in case custom @Admin controller (spring, I think) is defined
        configurer.addPathPrefix(oasApplicationServletProperties.adminPrefix,
                HandlerTypePredicate.forAnnotation(Admin::class.java))
    }

    @Bean
    @ConditionalOnMissingBean
    fun oasDispatchServlet(apiExecutionService: ApiExecutionService, apiDelayService: ApiDelayService, executorProvider: ExecutorProvider)
            = OasDispatchServlet(apiExecutionService, apiDelayService, executorProvider)

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

@ConfigurationProperties(prefix = "oas.stub.servlet")
data class OasApplicationServletProperties(var prefix: String = "/oas",
                                           var adminPrefix: String = "/__admin")

@ConfigurationProperties(prefix = "oas.stub.executors")
data class ExecutorsProperties(var parallelism: Int = Runtime.getRuntime().availableProcessors())
