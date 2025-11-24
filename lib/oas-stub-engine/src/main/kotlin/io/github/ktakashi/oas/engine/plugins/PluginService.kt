package io.github.ktakashi.oas.engine.plugins

import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.ktakashi.oas.api.http.RequestContext
import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.api.plugin.ApiPlugin
import io.github.ktakashi.oas.api.plugin.PluginContext
import io.github.ktakashi.oas.api.storage.Storage
import io.github.ktakashi.oas.engine.apis.ApiContextService
import io.github.ktakashi.oas.engine.models.mergeProperty
import io.github.ktakashi.oas.engine.storages.StorageService
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.PluginDefinition
import java.time.Duration
import java.util.Optional
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectReader
import tools.jackson.databind.ObjectWriter
import tools.jackson.databind.json.JsonMapper

private val logger = LoggerFactory.getLogger(PluginService::class.java)

class PluginService(private val pluginCompilers: Set<PluginCompiler>,
                    private val apiContextService: ApiContextService,
                    private val storageService: StorageService,
                    private val jsonMapper: JsonMapper
) {

    private val pluginCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build(CacheLoader<PluginDefinition, Class<ApiPlugin>?> {
                pluginCompilers.firstOrNull { c -> c.support(it.type) }?.compileScript(it.script)
            })
    fun applyPlugin(requestContext: RequestContext, responseContext: ResponseContext): Mono<ResponseContext> =
            storageService.getPluginDefinition(requestContext.applicationName, requestContext.method, requestContext.apiPath).flatMap { plugin ->
                logger.debug("Applying plugin -> {}", plugin)
                apiContextService.getApiContext(requestContext.applicationName, requestContext.apiPath, requestContext.method)
                    .mapNotNull { context -> context.mergeProperty(ApiCommonConfigurations<*>::data)?.asMap() }
                    .switchIfEmpty(Mono.defer { Mono.just(mapOf()) })
                    .mapNotNull { apiData ->
                        pluginCache[plugin]?.let { compiled ->
                            val code = compiled.getConstructor().newInstance()
                            val context = PluginContextData(requestContext, responseContext, storageService.sessionStorage, apiData!!, jsonMapper)
                            code.customize(context)
                        }
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
                             private val jsonMapper: JsonMapper) : PluginContext {
    override val objectReader: ObjectReader
        get() = jsonMapper.reader()

    override val objectWriter: ObjectWriter
        get() = jsonMapper.writer()

    override fun <T> getApiData(label: String, clazz: Class<T>): Optional<T & Any> = Optional.ofNullable(apiData[label]?.let { v ->
        clazz.cast(if (clazz.isAssignableFrom(v.javaClass)) {
            v // super class
        } else {
            checkSubclass(v, clazz)
        })
    })

    private fun <T> checkSubclass(v: Any, clazz: Class<T>) = when (v) {
        is String -> if (String::class.java.isAssignableFrom(clazz)) v else null
        is Number -> if (Number::class.java.isAssignableFrom(clazz)) v else null
        is ByteArray -> if (ByteArray::class.java.isAssignableFrom(clazz)) v else null
        is Map<*, *> -> jsonMapper.convertValue(v, clazz)
        else -> if (v.javaClass.isAssignableFrom(clazz)) v else null
    }

}
