package io.github.ktakashi.oas.engine.storages

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache

import io.github.ktakashi.oas.engine.apis.ApiPathService
import io.github.ktakashi.oas.engine.plugins.PluginDefinition
import io.github.ktakashi.oas.plugin.apis.Storage
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
                    val persistentStorage: Storage,
                    val sessionStorage: Storage) {
    private val externalData: LoadingCache<String, Optional<PersistentData>> = Caffeine.newBuilder()
            .build { k -> persistentStorage.get(k, PersistentData::class.java) }
    private val apiDefinitions: LoadingCache<String, Optional<OpenAPI>> = Caffeine.newBuilder()
            .build { k -> externalData[k]
                    .map(PersistentData::api)
                    .map { s -> openApiV3Parser.readContents(s).openAPI }
            }
    fun getApiDefinition(name: String): Optional<OpenAPI> = apiDefinitions[name]

    fun getPluginDefinition(name: String, path: String): Optional<PluginDefinition> = getPluginDefinitions(name)
            .flatMap { v -> apiPathService.findMatchingPath(path, v) }

    fun getApiData(name: String): Optional<Map<String, ByteArray>> = externalData[name].map { v -> v.apiData }
    private fun getPluginDefinitions(name: String) = externalData[name].map { v -> v.plugins }

}


private data class PersistentData(val api: String,
                                  val plugins: Map<String, PluginDefinition>,
                                  val apiData: Map<String, ByteArray>)
