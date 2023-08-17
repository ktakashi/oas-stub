package io.github.ktakashi.oas.engine.apis

import io.github.ktakashi.oas.engine.models.ModelPropertyUtils
import io.github.ktakashi.oas.engine.storages.StorageService
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.ApiDelay
import io.github.ktakashi.oas.model.ApiFixedDelay
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import kotlin.time.toTimeUnit

@Named @Singleton
class ApiDelayService
@Inject constructor(private val storageService: StorageService) {
    @JvmOverloads
    fun <T> delayFuture(apiContext: ApiContext, completableStage: CompletionStage<T>, executor: ExecutorService = ForkJoinPool.commonPool()) = System.currentTimeMillis().let { start ->
        ModelPropertyUtils.mergeProperty(apiContext.apiPath, apiContext.apiDefinitions, ApiCommonConfigurations<*>::delay)?.let { config ->
            completableStage.thenCompose { r ->
                computeDelay(config, System.currentTimeMillis() - start)?.let { (delay, timeUnit) ->
                    val delayExecutor = CompletableFuture.delayedExecutor(delay, timeUnit, executor)
                    CompletableFuture.supplyAsync({ r }, delayExecutor)
                } ?: CompletableFuture.completedFuture(r)
            }
        } ?: completableStage
    }

    fun computeDelay(context: String, path: String, processTime: Long) = storageService.getApiDefinitions(context).map { def ->
        ModelPropertyUtils.mergeProperty(path, def, ApiCommonConfigurations<*>::delay)?.let { config ->
            computeDelay(config, processTime)
        }
    }.orElse(null)

    private fun computeDelay(config: ApiDelay, processTime: Long): Pair<Long, TimeUnit>? = when (config) {
        is ApiFixedDelay -> {
            val unit = config.delayDurationUnit?: ApiDelay.DEFAULT_DURATION_UNIT
            val delay = unit.toTimeUnit().toMillis(config.fixedDelay)
            if (processTime < delay) {
                delay - processTime to TimeUnit.MILLISECONDS
            } else {
                null
            }
        }
    }
}
