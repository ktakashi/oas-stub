package io.github.ktakashi.oas.engine.plugins

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.ktakashi.oas.engine.models.ModelPropertyUtils
import io.github.ktakashi.oas.engine.storages.StorageService
import io.github.ktakashi.oas.model.ApiCommonConfigurations
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
import kotlin.math.log
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(PluginService::class.java)

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
                logger.debug("Applying plugin -> {}", plugin)
                try {
                    val compiled = pluginCache[plugin]
                    val apiData: Optional<Map<String, Any>> = storageService.getApiDefinitions(requestContext.applicationName)
                            .map { v -> ModelPropertyUtils.mergeProperty(requestContext.apiPath, v, ApiCommonConfigurations<*>::data) }
                            // The null-safe operator must be redundant, but as of Kotlin 1.9.0 doesn't detect it...
                            .map { it?.asMap()}
                    val code = compiled.getConstructor().newInstance()
                    val context = PluginContextData(requestContext, responseContext,
                            storageService.sessionStorage,
                            apiData.orElseGet { mapOf() },
                            objectMapper)
                    code.customize(context)
                } catch (e: Exception) {
                    logger.info("Failed execute plugin: {}", e.message, e)
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
