package io.github.ktakashi.oas.engine.storages

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import io.github.ktakashi.oas.engine.parsers.ParsingService
import io.github.ktakashi.oas.engine.paths.findMatchingPathValue
import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.api.storage.Storage
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.swagger.v3.oas.models.OpenAPI
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


class StorageService(private val parsingService: ParsingService,
                     private val persistentStorage: PersistentStorage,
                     val sessionStorage: Storage
) {
    private val apiDefinitions: LoadingCache<String, Mono<ApiDefinitions>> = Caffeine.newBuilder()
            .build { k -> Mono.defer { Mono.justOrEmpty(persistentStorage.getApiDefinition(k)) } }
    private val openApiCache: LoadingCache<String, Mono<OpenAPI>> = Caffeine.newBuilder()
            .build { k -> apiDefinitions[k]
                    .mapNotNull<String>(ApiDefinitions::specification)
                    .flatMap(parsingService::parse)
            }

    fun saveApiDefinitions(name: String, definitions: ApiDefinitions): ApiDefinitions? = persistentStorage.setApiDefinition(name, definitions).let {
        apiDefinitions.invalidate(name)
        openApiCache.invalidate(name)
        if (it) definitions else null
    }

    fun deleteApiDefinitions(name: String): Boolean = persistentStorage.deleteApiDefinition(name).also {
        apiDefinitions.invalidate(name)
        openApiCache.invalidate(name)
    }

    fun getApiDefinitions(name: String): Mono<ApiDefinitions> = apiDefinitions[name]
    fun getOpenApi(name: String): Mono<OpenAPI> = openApiCache[name]

    fun getPluginDefinition(name: String, path: String): Mono<PluginDefinition> = apiDefinitions[name]
            .mapNotNull { v -> v.configurations }
            .flatMap { v -> Mono.justOrEmpty(findMatchingPathValue(path, v as Map<String, ApiConfiguration>)) }
            .mapNotNull { v -> v.plugin }

    fun getApiNames(): Flux<String> = Flux.defer { Flux.fromIterable(persistentStorage.getNames()) }
}
