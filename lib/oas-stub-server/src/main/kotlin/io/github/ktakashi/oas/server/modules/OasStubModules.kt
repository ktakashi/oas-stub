package io.github.ktakashi.oas.server.modules

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
import io.github.ktakashi.oas.engine.apis.DefaultApiRegistrationService
import io.github.ktakashi.oas.engine.apis.DefaultApiService
import io.github.ktakashi.oas.engine.apis.json.JsonOpenApi30DataPopulator
import io.github.ktakashi.oas.engine.apis.json.JsonOpenApi31DataPopulator
import io.github.ktakashi.oas.engine.apis.json.JsonOpenApi30DataValidator
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
import io.github.ktakashi.oas.api.storage.Storage
import io.github.ktakashi.oas.server.options.OasStubServerStubOptions
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val validatorModule = module {
    singleOf(::FormatValidator) {
        bind<Validator<*>>()
    }
    singleOf(::LocalDateValidator) {
        bind<Validator<*>>()
    }
    singleOf(::OffsetDateValidator) {
        bind<Validator<*>>()
    }
    singleOf(::UUIDValidator) {
        bind<Validator<*>>()
    }
    singleOf(::EmailValidator) {
        bind<Validator<*>>()
    }

    single { ApiRequestBodyValidator(getAll<ApiDataValidator<JsonNode>>().toSet()) } bind ApiRequestValidator::class
    single { ApiRequestParameterValidator(getAll<ApiDataValidator<JsonNode>>().toSet()) } bind ApiRequestValidator::class
    singleOf(::ApiRequestPathVariableValidator) {
        bind<ApiRequestValidator>()
    }
    singleOf(::ApiRequestSecurityValidator) {
        bind<ApiRequestValidator>()
    }

    single { JsonOpenApi30DataValidator(get<ObjectMapper>(), getAll<Validator<Any>>().toSet()) } bind ApiDataValidator::class
    single { JsonOpenApi31DataValidator(get<ObjectMapper>(), getAll<Validator<Any>>().toSet()) } bind ApiDataValidator::class
    singleOf(::JsonOpenApi30DataPopulator) {
        bind<ApiDataPopulator>()
        bind<ApiAnyDataPopulator>()
    }
    singleOf(::JsonOpenApi31DataPopulator) {
        bind<ApiDataPopulator>()
        bind<ApiAnyDataPopulator>()
    }
}

fun makeEngineModule(options: OasStubServerStubOptions) = module {
    singleOf(::ParsingService)
    singleOf(::StorageService)
    single { ApiContentDecider(getAll<ApiRequestValidator>().toSet(), get<ObjectMapper>()) }
    single { ApiResultProvider(get<ApiContentDecider>(), getAll<ApiDataPopulator>().toSet(), getAll<ApiAnyDataPopulator>().toSet()) }
    singleOf(::DefaultApiService) {
        bind<ApiExecutionService>()
    }
    singleOf(::DefaultApiRegistrationService) {
        bind<ApiRegistrationService>()
    }
    singleOf(::ApiObserver)
    singleOf(::ApiDelayService)

    singleOf(::GroovyPluginCompiler) {
        bind<PluginCompiler>()
    }

    single { PluginService(getAll<PluginCompiler>().toSet(), get<StorageService>(), get<ObjectMapper>()) }
    single { ApiPathService(options.stubPath) }
    single { options.objectMapper }
}

internal fun makeStorageModule(persistentStorage: PersistentStorage, sessionStorage: SessionStorage) = module {
    single<PersistentStorage> { persistentStorage }
    single<SessionStorage> { sessionStorage } bind Storage::class
}