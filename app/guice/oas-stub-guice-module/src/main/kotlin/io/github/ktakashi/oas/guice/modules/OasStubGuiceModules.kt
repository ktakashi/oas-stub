package io.github.ktakashi.oas.guice.modules

import com.fasterxml.jackson.databind.JsonNode
import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.multibindings.Multibinder
import com.google.inject.name.Names
import com.google.inject.servlet.ServletModule
import com.google.inject.util.Types
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
import io.github.ktakashi.oas.engine.apis.json.JsonOpenApi30DataPopulator
import io.github.ktakashi.oas.engine.apis.json.JsonOpenApi30DataValidator
import io.github.ktakashi.oas.engine.apis.json.JsonOpenApi31DataPopulator
import io.github.ktakashi.oas.engine.apis.json.JsonOpenApi31DataValidator
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.engine.parsers.ParsingService
import io.github.ktakashi.oas.engine.plugins.PluginCompiler
import io.github.ktakashi.oas.engine.plugins.PluginService
import io.github.ktakashi.oas.engine.plugins.groovy.GroovyPluginCompiler
import io.github.ktakashi.oas.engine.storages.StorageService
import io.github.ktakashi.oas.engine.validators.EmailValidator
import io.github.ktakashi.oas.engine.validators.FormatValidator
import io.github.ktakashi.oas.engine.validators.LocalDateValidator
import io.github.ktakashi.oas.engine.validators.OffsetDateValidator
import io.github.ktakashi.oas.engine.validators.UUIDValidator
import io.github.ktakashi.oas.engine.validators.Validator
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceConfiguration
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceServerConfiguration
import io.github.ktakashi.oas.guice.server.OasStubServer
import io.github.ktakashi.oas.guice.services.DefaultExecutorProvider
import io.github.ktakashi.oas.guice.storages.apis.OasStubPersistentStorageModule
import io.github.ktakashi.oas.guice.storages.apis.OasStubSessionStorageModule
import io.github.ktakashi.oas.jersey.OAS_APPLICATION_PATH_CONFIG
import io.github.ktakashi.oas.plugin.apis.Storage
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.storages.inmemory.InMemoryPersistentStorage
import io.github.ktakashi.oas.storages.inmemory.InMemorySessionStorage
import io.github.ktakashi.oas.web.aspects.DelayableAspect
import io.github.ktakashi.oas.web.services.ExecutorProvider
import io.github.ktakashi.oas.web.servlets.OasDispatchServlet


class OasStubInMemorySessionStorageModule: AbstractModule(), OasStubSessionStorageModule {
    override fun configure() {
        val storage = InMemorySessionStorage()
        bind(Storage::class.java).toInstance(storage)
        bind(SessionStorage::class.java).toInstance(storage)
    }

}

class OasStubInMemoryPersistentStorageModule: AbstractModule(), OasStubPersistentStorageModule {
    override fun configure() {
        bind(PersistentStorage::class.java).to(InMemoryPersistentStorage::class.java)
    }
}

class OasStubGuiceEngineModule(private val configuration: OasStubGuiceConfiguration): AbstractModule() {
    override fun configure() {
        install(configuration.sessionStorageModule)
        install(configuration.persistentStorageModule)
        bind(OasStubGuiceConfiguration::class.java).toInstance(configuration)
        bind(ParsingService::class.java)
        bind(StorageService::class.java)
        bind(PluginService::class.java)
        configurePluginCompiler()
        configureValidators()
        bindConstant().annotatedWith(Names.named(API_PATH_NAME_QUALIFIER)).to(configuration.oasStubConfiguration.servletPrefix)
        bind(ApiContentDecider::class.java)
        bind(ApiDelayService::class.java)
        bind(ApiPathService::class.java)
        bind(ApiResultProvider::class.java)
        bind(ApiExecutionService::class.java).to(DefaultApiService::class.java)
        bind(ApiRegistrationService::class.java).to(DefaultApiService::class.java)
        bind(ApiObserver::class.java)
    }

    private fun configurePluginCompiler() {
        val compiler = Multibinder.newSetBinder(binder(), PluginCompiler::class.java)
        compiler.addBinding().to(GroovyPluginCompiler::class.java)
    }

    private fun configureValidators() {
        val validators = Multibinder.newSetBinder(binder(),Key.get(Types.newParameterizedType(Validator::class.java, Any::class.java))) as Multibinder<Validator<*>>
        validators.addBinding().to(FormatValidator::class.java)
        validators.addBinding().to(LocalDateValidator::class.java)
        validators.addBinding().to(OffsetDateValidator::class.java)
        validators.addBinding().to(UUIDValidator::class.java)
        validators.addBinding().to(EmailValidator::class.java)

        bind(ApiRequestPathVariableValidator::class.java)

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
    }

    private fun bindDataValidator(dataValidator: Multibinder<ApiDataValidator<*>>) {
        dataValidator.addBinding().to(JsonOpenApi30DataValidator::class.java)
        dataValidator.addBinding().to(JsonOpenApi31DataValidator::class.java)
    }

}

class OasStubGuiceServletModule(private val configuration: OasStubGuiceConfiguration): ServletModule() {
    override fun configureServlets() {
        serve("${configuration.oasStubConfiguration.servletPrefix}/*").with(OasDispatchServlet::class.java)
    }
}

class OasStubGuiceWebModule(private val configuration: OasStubGuiceConfiguration): AbstractModule() {
    override fun configure() {
        install(OasStubGuiceServletModule(configuration))
        install(OasStubGuiceEngineModule(configuration))

        bind(ExecutorProvider::class.java).to(DefaultExecutorProvider::class.java)
        bindConstant().annotatedWith(Names.named(OAS_APPLICATION_PATH_CONFIG)).to(configuration.oasStubConfiguration.adminPrefix)

        requestInjection(DelayableAspect::class.java)
    }
}

class OasStubGuiceServerModule(private val configuration: OasStubGuiceServerConfiguration): AbstractModule() {
    override fun configure() {
        install(OasStubGuiceWebModule(configuration))
        val injectorProvider = getProvider(Injector::class.java)
        val server = OasStubServer(configuration, injectorProvider::get)
        bind(OasStubServer::class.java).toInstance(server)
        bind(OasStubGuiceServerConfiguration::class.java).toInstance(configuration)
    }
}