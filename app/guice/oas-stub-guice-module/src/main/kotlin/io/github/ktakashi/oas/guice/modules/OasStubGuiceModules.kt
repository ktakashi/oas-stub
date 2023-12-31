package io.github.ktakashi.oas.guice.modules

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.inject.AbstractModule
import com.google.inject.Key
import com.google.inject.matcher.Matchers
import com.google.inject.multibindings.Multibinder
import com.google.inject.name.Names
import com.google.inject.servlet.ServletModule
import com.google.inject.util.Types
import io.github.ktakashi.oas.engine.apis.API_PATH_NAME_QUALIFIER
import io.github.ktakashi.oas.engine.apis.ApiAnyDataPopulator
import io.github.ktakashi.oas.engine.apis.ApiDataPopulator
import io.github.ktakashi.oas.engine.apis.ApiDataValidator
import io.github.ktakashi.oas.engine.apis.ApiExecutionService
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.engine.apis.ApiRequestBodyValidator
import io.github.ktakashi.oas.engine.apis.ApiRequestParameterValidator
import io.github.ktakashi.oas.engine.apis.ApiRequestPathVariableValidator
import io.github.ktakashi.oas.engine.apis.ApiRequestSecurityValidator
import io.github.ktakashi.oas.engine.apis.ApiRequestValidator
import io.github.ktakashi.oas.engine.apis.DefaultApiService
import io.github.ktakashi.oas.engine.apis.json.JsonOpenApi30DataPopulator
import io.github.ktakashi.oas.engine.apis.json.JsonOpenApi30DataValidator
import io.github.ktakashi.oas.engine.apis.json.JsonOpenApi31DataPopulator
import io.github.ktakashi.oas.engine.apis.json.JsonOpenApi31DataValidator
import io.github.ktakashi.oas.engine.plugins.PluginCompiler
import io.github.ktakashi.oas.engine.plugins.groovy.GroovyPluginCompiler
import io.github.ktakashi.oas.engine.validators.EmailValidator
import io.github.ktakashi.oas.engine.validators.FormatValidator
import io.github.ktakashi.oas.engine.validators.LocalDateValidator
import io.github.ktakashi.oas.engine.validators.OffsetDateValidator
import io.github.ktakashi.oas.engine.validators.UUIDValidator
import io.github.ktakashi.oas.engine.validators.Validator
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceConfiguration
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceServerConfiguration
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceWebConfiguration
import io.github.ktakashi.oas.guice.services.DefaultExecutorProvider
import io.github.ktakashi.oas.guice.services.DelayableInterceptor
import io.github.ktakashi.oas.guice.storages.apis.OasStubPersistentStorageModuleCreator
import io.github.ktakashi.oas.guice.storages.apis.OasStubSessionStorageModuleCreator
import io.github.ktakashi.oas.jersey.OAS_APPLICATION_PATH_CONFIG
import io.github.ktakashi.oas.plugin.apis.Storage
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.storages.inmemory.InMemoryPersistentStorage
import io.github.ktakashi.oas.storages.inmemory.InMemorySessionStorage
import io.github.ktakashi.oas.web.annotations.Delayable
import io.github.ktakashi.oas.web.services.ExecutorProvider
import io.github.ktakashi.oas.web.servlets.OasDispatchServlet


class OasStubInMemorySessionStorageModule private constructor(): AbstractModule() {
    companion object {
        @JvmField
        val CREATOR = OasStubSessionStorageModuleCreator {
            OasStubInMemorySessionStorageModule()
        }
    }
    override fun configure() {
        val storage = InMemorySessionStorage()
        bind(Storage::class.java).toInstance(storage)
        bind(SessionStorage::class.java).toInstance(storage)
    }
}


class OasStubInMemoryPersistentStorageModule private constructor(): AbstractModule() {
    companion object {
        @JvmField
        val CREATOR = OasStubPersistentStorageModuleCreator {
            OasStubInMemoryPersistentStorageModule()
        }
    }
    override fun configure() {
        bind(PersistentStorage::class.java).to(InMemoryPersistentStorage::class.java)
    }
}

class OasStubGuiceEngineModule(private val configuration: OasStubGuiceConfiguration): AbstractModule() {
    override fun configure() {
        val jacksonBuilder = JsonMapper.builder()
            .addModules(KotlinModule.Builder().build())
            .addModules(JavaTimeModule())
            .addModules(Jdk8Module())
        configuration.objectMapperBuilderCustomizer.customize(jacksonBuilder)
        val objectMapper = jacksonBuilder.build()
        bind(ObjectMapper::class.java).toInstance(objectMapper)

        install(configuration.sessionStorageModuleCreator.createSessionStorage(objectMapper))
        install(configuration.persistentStorageModuleCreator.createPersistentStorage(objectMapper))

        bind(OasStubGuiceConfiguration::class.java).toInstance(configuration)
        configurePluginCompiler()
        configureValidators()
        bindConstant().annotatedWith(Names.named(API_PATH_NAME_QUALIFIER)).to(configuration.oasStubConfiguration.servletPrefix)
        bind(ApiExecutionService::class.java).to(DefaultApiService::class.java)
        bind(ApiRegistrationService::class.java).to(DefaultApiService::class.java)
    }

    private fun configurePluginCompiler() {
        val compiler = Multibinder.newSetBinder(binder(), PluginCompiler::class.java)
        compiler.addBinding().to(GroovyPluginCompiler::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    private fun configureValidators() {
        val validators = Multibinder.newSetBinder(binder(),Key.get(Types.newParameterizedType(Validator::class.java, Any::class.java))) as Multibinder<Validator<*>>
        validators.addBinding().to(FormatValidator::class.java)
        validators.addBinding().to(LocalDateValidator::class.java)
        validators.addBinding().to(OffsetDateValidator::class.java)
        validators.addBinding().to(UUIDValidator::class.java)
        validators.addBinding().to(EmailValidator::class.java)

        val jsonNodeValidator = Multibinder.newSetBinder(binder(), Key.get(Types.newParameterizedType(ApiDataValidator::class.java, JsonNode::class.java))) as Multibinder<ApiDataValidator<*>>
        bindDataValidator(jsonNodeValidator)
        val dataValidator = Multibinder.newSetBinder(binder(), Key.get(Types.newParameterizedType(ApiDataValidator::class.java, Any::class.java))) as Multibinder<ApiDataValidator<*>>
        bindDataValidator(dataValidator)

        val dataPopulator = Multibinder.newSetBinder(binder(), ApiDataPopulator::class.java)
        dataPopulator.addBinding().to(JsonOpenApi30DataPopulator::class.java)
        dataPopulator.addBinding().to(JsonOpenApi31DataPopulator::class.java)

        val anyDataPopulator = Multibinder.newSetBinder(binder(), ApiAnyDataPopulator::class.java)
        anyDataPopulator.addBinding().to(JsonOpenApi30DataPopulator::class.java)
        anyDataPopulator.addBinding().to(JsonOpenApi31DataPopulator::class.java)

        val apiRequestValidatorBinder = Multibinder.newSetBinder(binder(), ApiRequestValidator::class.java)
        apiRequestValidatorBinder.addBinding().to(ApiRequestParameterValidator::class.java)
        apiRequestValidatorBinder.addBinding().to(ApiRequestSecurityValidator::class.java)
        apiRequestValidatorBinder.addBinding().to(ApiRequestBodyValidator::class.java)
        apiRequestValidatorBinder.addBinding().to(ApiRequestPathVariableValidator::class.java)
    }

    private fun bindDataValidator(dataValidator: Multibinder<ApiDataValidator<*>>) {
        dataValidator.addBinding().to(JsonOpenApi30DataValidator::class.java)
        dataValidator.addBinding().to(JsonOpenApi31DataValidator::class.java)
    }

}

class OasStubGuiceServletModule(private val configuration: OasStubGuiceWebConfiguration): ServletModule() {
    override fun configureServlets() {
        serve("${configuration.oasStubConfiguration.servletPrefix}/*").with(OasDispatchServlet::class.java)
    }
}

class OasStubGuiceWebModule(private val configuration: OasStubGuiceWebConfiguration): AbstractModule() {
    override fun configure() {
        install(OasStubGuiceServletModule(configuration))
        install(OasStubGuiceEngineModule(configuration))

        bind(OasStubGuiceWebConfiguration::class.java).toInstance(configuration)
        bind(ExecutorProvider::class.java).to(DefaultExecutorProvider::class.java)
        bindConstant().annotatedWith(Names.named(OAS_APPLICATION_PATH_CONFIG)).to(configuration.oasStubConfiguration.adminPrefix)

        val delayableInterceptor = DelayableInterceptor()
        requestInjection(delayableInterceptor)
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Delayable::class.java), delayableInterceptor)
        bind(DelayableInterceptor::class.java).toInstance(delayableInterceptor)
    }
}

class OasStubGuiceServerModule(private val configuration: OasStubGuiceServerConfiguration): AbstractModule() {
    override fun configure() {
        install(OasStubGuiceWebModule(configuration))
        bind(OasStubGuiceServerConfiguration::class.java).toInstance(configuration)
    }
}