package io.github.ktakashi.oas.engine.apis

import io.github.ktakashi.oas.engine.models.mergeProperty
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.ApiDelay
import io.github.ktakashi.oas.model.ApiFixedDelay
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import kotlin.time.toTimeUnit
import reactor.core.publisher.Mono

class ApiDelayService(private val apiContextService: ApiContextService) {
    @JvmOverloads
    fun <T> delayFuture(apiContext: ApiContext, completableStage: CompletionStage<T>, executor: ExecutorService = ForkJoinPool.commonPool()) = System.currentTimeMillis().let { start ->
        apiContext.mergeProperty(ApiCommonConfigurations<*>::delay)?.let { config ->
            completableStage.thenCompose { r ->
                computeDelay(config, System.currentTimeMillis() - start)?.let { (delay, timeUnit) ->
                    val delayExecutor = CompletableFuture.delayedExecutor(delay, timeUnit, executor)
                    CompletableFuture.supplyAsync({ r }, delayExecutor)
                } ?: CompletableFuture.completedFuture(r)
            }
        } ?: completableStage
    }

    fun <T> delayMono(apiContext: ApiContext, mono: Mono<T>) = System.currentTimeMillis().let { start ->
        apiContext.mergeProperty(ApiCommonConfigurations<*>::delay)?.let { config ->
            computeDelay(config, System.currentTimeMillis() - start)?.let { (delay, timeUnit) ->
                mono.delayElement(Duration.of(delay, timeUnit.toChronoUnit()))
            }
        } ?: mono
    }

    fun <T> delayMono(context: String, method: String, path: String, mono: Mono<T>) = System.currentTimeMillis().let { start ->
        apiContextService.getApiContext(context, path, method).flatMap { context ->
            context.mergeProperty(ApiCommonConfigurations<*>::delay)?.let { config ->
                computeDelay(config, System.currentTimeMillis() - start)?.let { (delay, timeUnit) ->
                    mono.delayElement(Duration.of(delay, timeUnit.toChronoUnit()))
                }
            } ?: mono
        }.switchIfEmpty(mono)
    }

    fun computeDelay(context: String, method: String, path: String, processTime: Long) = apiContextService.getApiContext(context, method, path).flatMap { context ->
        context.mergeProperty(ApiCommonConfigurations<*>::delay)?.let { config ->
            Mono.justOrEmpty(computeDelay(config, processTime))
        } ?: Mono.empty()
    }

    private fun computeDelay(config: ApiDelay, processTime: Long): Pair<Long, TimeUnit>? = when (config) {
        is ApiFixedDelay -> {
            val unit = config.delayUnit?: ApiDelay.DEFAULT_DURATION_UNIT
            val delay = unit.toTimeUnit().toMillis(config.delay)
            if (processTime < delay) {
                delay - processTime to TimeUnit.MILLISECONDS
            } else {
                null
            }
        }
    }
}
