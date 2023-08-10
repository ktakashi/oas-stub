package io.github.ktakashi.oas.engine.plugins

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.ktakashi.oas.engine.storages.StorageService
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.plugin.apis.ApiPlugin
import io.github.ktakashi.oas.plugin.apis.PluginContext
import io.github.ktakashi.oas.plugin.apis.RequestContext
import io.github.ktakashi.oas.plugin.apis.ResponseContext
import io.github.ktakashi.oas.plugin.apis.Storage
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.time.Duration
import java.util.Optional

@Named @Singleton
class PluginService
@Inject constructor(private val pluginCompilers: Set<PluginCompiler>,
                    private val storageService: StorageService,
                    private val objectMapper: ObjectMapper) {

    private val pluginCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build(CacheLoader<PluginDefinition, Class<ApiPlugin>> {
                pluginCompilers.firstOrNull { c -> c.support(it.type) }?.compileScript(it.script)
            })
    fun applyPlugin(requestContext: RequestContext, responseContext: ResponseContext): ResponseContext =
            storageService.getPluginDefinition(requestContext.applicationName, requestContext.apiPath).map { plugin ->
                try {
                    val compiled = pluginCache[plugin]
                    val stubData = storageService.getApiData(requestContext.applicationName)
                    val code = compiled.getConstructor().newInstance()
                    val context = PluginContextData(requestContext, responseContext,
                            storageService.sessionStorage,
                            stubData.orElseGet { mapOf() },
                            objectMapper)
                    code.customize(context)
                } catch (e: Exception) {
                    responseContext
                }
            }.orElse(responseContext)
}

data class PluginContextData(override val requestContext: RequestContext,
                             override val responseContext: ResponseContext,
                             override val sessionStorage: Storage,
                             private val apiData: Map<String, Any>,
                             private val objectMapper: ObjectMapper) :PluginContext {
    override fun <T> getApiData(label: String, clazz: Class<T>): Optional<T & Any> =
            clazz.cast(apiData[label])?.let { v ->
                when (v) {
                    is String -> if (String::class.java.isAssignableFrom(clazz)) Optional.of(v) else null
                    is Number -> if (Number::class.java.isAssignableFrom(clazz)) Optional.of(v) else null
                    is ByteArray -> if (ByteArray::class.java.isAssignableFrom(clazz)) Optional.of(v) else null
                    is Map<*, *> -> Optional.ofNullable(objectMapper.convertValue(v, clazz))
                    else -> null // unknown type for now
                }
            }?: Optional.empty()
}
