package io.github.ktakashi.oas.engine.plugins

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.ktakashi.oas.engine.models.ModelPropertyUtils
import io.github.ktakashi.oas.engine.storages.StorageService
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.api.plugin.PluginContext
import io.github.ktakashi.oas.api.http.RequestContext
import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.api.plugin.ApiPlugin
import io.github.ktakashi.oas.api.storage.Storage
import java.time.Duration
import java.util.Optional
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

private val logger = LoggerFactory.getLogger(PluginService::class.java)

class PluginService(private val pluginCompilers: Set<PluginCompiler>,
                    private val storageService: StorageService,
                    private val objectMapper: ObjectMapper) {

    private val pluginCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build(CacheLoader<PluginDefinition, Class<ApiPlugin>> {
                pluginCompilers.firstOrNull { c -> c.support(it.type) }?.compileScript(it.script)
            })
    fun applyPlugin(requestContext: RequestContext, responseContext: ResponseContext): Mono<ResponseContext> =
            storageService.getPluginDefinition(requestContext.applicationName, requestContext.apiPath).flatMap { plugin ->
                logger.debug("Applying plugin -> {}", plugin)
                storageService.getApiDefinitions(requestContext.applicationName)
                    .flatMap { v -> Mono.justOrEmpty(ModelPropertyUtils.mergeProperty(requestContext.apiPath, v, ApiCommonConfigurations<*>::data)) }
                    .map { it.asMap()}
                    .switchIfEmpty(Mono.defer { Mono.just(mapOf()) })
                    .map { apiData ->
                        val compiled = pluginCache[plugin]
                        val code = compiled.getConstructor().newInstance()
                        val context = PluginContextData(requestContext, responseContext, storageService.sessionStorage, apiData, objectMapper)
                        code.customize(context)
                    }.onErrorResume { e ->
                        logger.info("Failed execute plugin: {}", e.message, e)
                        Mono.empty()
                    }
            }.switchIfEmpty(Mono.just(responseContext))
}

data class PluginContextData(override val requestContext: RequestContext,
                             override val responseContext: ResponseContext,
                             override val sessionStorage: Storage,
                             private val apiData: Map<String, Any>,
                             private val objectMapper: ObjectMapper) : PluginContext {
    override val objectReader: ObjectReader
        get() = objectMapper.reader()

    override val objectWriter: ObjectWriter
        get() = objectMapper.writer()

    override fun <T> getApiData(label: String, clazz: Class<T>): Optional<T & Any> =
            Optional.ofNullable(apiData[label]?.let { v ->
                clazz.cast(when (v) {
                    is String -> if (String::class.java.isAssignableFrom(clazz)) v else null
                    is Number -> if (Number::class.java.isAssignableFrom(clazz)) v else null
                    is ByteArray -> if (ByteArray::class.java.isAssignableFrom(clazz)) v else null
                    is Map<*, *> -> objectMapper.convertValue(v, clazz)
                    else -> if (v.javaClass.isAssignableFrom(clazz)) v else null
                })
            })

}
