package io.github.ktakashi.oas.engine.storages

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import io.github.ktakashi.oas.engine.apis.ApiPathService
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.plugin.apis.Storage
import io.github.ktakashi.oas.storage.apis.PersistentStorage
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.util.Optional


private val openApiV3Parser = OpenAPIV3Parser()


@Named @Singleton
class StorageService
@Inject constructor(private val apiPathService: ApiPathService,
                    private val persistentStorage: PersistentStorage,
                    val sessionStorage: Storage) {
    private val apiDefinitions: LoadingCache<String, Optional<ApiDefinitions>> = Caffeine.newBuilder()
            .build { k -> persistentStorage.getApiDefinition(k) }
    private val openApiCache: LoadingCache<String, Optional<OpenAPI>> = Caffeine.newBuilder()
            .build { k -> apiDefinitions[k]
                    .map(ApiDefinitions::api)
                    .map { s -> openApiV3Parser.readContents(s).openAPI }
            }

    fun saveApiDefinitions(name: String, definitions: ApiDefinitions) {
        persistentStorage.setApiDefinition(name, definitions)
        apiDefinitions.invalidate(name)
        openApiCache.invalidate(name)
    }

    fun getApiDefinitions(name: String): Optional<ApiDefinitions> = apiDefinitions[name]
    fun getOpenApi(name: String): Optional<OpenAPI> = openApiCache[name]

    fun getPluginDefinition(name: String, path: String): Optional<PluginDefinition> = apiDefinitions[name]
            .flatMap { v -> apiPathService.findMatchingPath(path, v.apiConfigurations) }
            .flatMap { v -> v.plugin }

    fun getApiData(name: String): Optional<Map<String, Any>> = apiDefinitions[name].map { v -> v.apiData }

}
