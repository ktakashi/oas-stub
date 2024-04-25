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
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.engine.apis.ApiRequestBodyValidator
import io.github.ktakashi.oas.engine.apis.ApiRequestParameterValidator
import io.github.ktakashi.oas.engine.apis.ApiRequestPathVariableValidator
import io.github.ktakashi.oas.engine.apis.ApiRequestSecurityValidator
import io.github.ktakashi.oas.engine.apis.ApiRequestValidator
import io.github.ktakashi.oas.engine.apis.ApiResultProvider
import io.github.ktakashi.oas.engine.apis.DefaultApiService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.engine.parsers.ParsingService
import io.github.ktakashi.oas.engine.plugins.PluginCompiler
import io.github.ktakashi.oas.engine.plugins.PluginService
import io.github.ktakashi.oas.engine.plugins.groovy.GroovyPluginCompiler
import io.github.ktakashi.oas.engine.storages.StorageService
import io.github.ktakashi.oas.services.DefaultExecutorProvider
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.storages.inmemory.configurations.AutoInMemoryPersistentStorageConfiguration
import io.github.ktakashi.oas.storages.inmemory.configurations.AutoInMemorySessionStorageConfiguration
import io.github.ktakashi.oas.web.services.ExecutorProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


// *sigh*...
@AutoConfiguration(
    // At least these are needed, if others are also there, then that's also fine
    after = [AutoInMemoryPersistentStorageConfiguration::class, AutoInMemorySessionStorageConfiguration::class]
)
@Configuration
class AutoOasStorageConfiguration {
    // Because of this dependency, we can't use component scan
    // Though, spring boot official document says, don't mex with autoconfiguration...
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
    fun apiRequestPathVariableValidator(requestParameterValidator: ApiRequestParameterValidator)
            = ApiRequestPathVariableValidator(requestParameterValidator)
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
                   apiResultProvider: ApiResultProvider,
                   pluginService: PluginService): ApiExecutionService
            = DefaultApiService(storageService, parsingService(), apiPathService, apiResultProvider, pluginService)

    @Bean
    @ConditionalOnMissingBean
    fun apiObserver(sessionStorage: SessionStorage) = ApiObserver(sessionStorage)
}

@AutoConfiguration(after = [AutoOasEngineConfiguration::class])
@Configuration
@EnableConfigurationProperties(ExecutorsProperties::class)
class AutoOasWebCoreConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun executorProvider(executorsProperties: ExecutorsProperties): ExecutorProvider = DefaultExecutorProvider(executorsProperties)

}

@ConfigurationProperties(prefix = "oas.stub.servlet")
data class OasApplicationServletProperties(var prefix: String = "/oas",
                                           var adminPrefix: String = "/__admin")

@ConfigurationProperties(prefix = "oas.stub.executors")
data class ExecutorsProperties(var parallelism: Int = Runtime.getRuntime().availableProcessors())
