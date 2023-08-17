package io.github.ktakashi.oas.engine.apis

import io.github.ktakashi.oas.engine.models.ModelPropertyUtils
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.ApiDelay
import io.github.ktakashi.oas.model.ApiFixedDelay
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit
import kotlin.time.toTimeUnit

@Named @Singleton
class ApiDelayService
@Inject constructor() {
    fun <T> delayFuture(apiContext: ApiContext, completableStage: CompletionStage<T>) = System.currentTimeMillis().let { start ->
        ModelPropertyUtils.mergeProperty(apiContext.apiPath, apiContext.apiDefinitions, ApiCommonConfigurations<*>::delay)?.let { config ->
            completableStage.thenCompose { r ->
                computeDelay(config, System.currentTimeMillis() - start)?.let { (delay, timeUnit) ->
                    // TODO we should provide parent executor instead of using ASYNC_POOL
                    val executor = CompletableFuture.delayedExecutor(delay, timeUnit)
                    CompletableFuture.supplyAsync({ r }, executor)
                } ?: CompletableFuture.completedFuture(r)
            }
        } ?: completableStage
    }

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
