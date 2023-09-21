package io.github.ktakashi.oas.web.aspects

import io.github.ktakashi.oas.engine.apis.ApiDelayService
import io.github.ktakashi.oas.web.annotations.Delayable
import io.github.ktakashi.oas.web.services.ExecutorProvider
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

private const val EXECUTOR_NAME = "delayableAspect"
@Aspect
@Named @Singleton
class DelayableAspect
@Inject constructor(private val apiDelayService: ApiDelayService,
                    executorProvider: ExecutorProvider) {
    private val executor = executorProvider.getExecutor(EXECUTOR_NAME)
    // Unfortunately, this AspectJ expression works only on Spring... :(
    @Around("@annotation(io.github.ktakashi.oas.web.annotations.Delayable) && @annotation(delayable)")
    fun delayAround(jointPoint: ProceedingJoinPoint, delayable: Delayable): Any? {
        val start = System.currentTimeMillis()
        val r = jointPoint.proceed()
        return proceedDelay(r, delayable, start)
    }

    fun proceedDelay(r: Any?, delayable: Delayable, start: Long): Any? = when (r) {
        is Mono<*> -> r.flatMap { v ->
            Mono.just(v)
                .delayElement(doDelay(delayable, start) { (delay, unit) ->
                    Duration.of(delay, unit.toChronoUnit())
                } ?: Duration.ZERO)
        }

        is Flux<*> -> r.flatMap { v ->
            Mono.just(v)
                .delayElement(doDelay(delayable, start) { (delay, unit) ->
                    Duration.of(delay, unit.toChronoUnit())
                } ?: Duration.ZERO)
        }

        is CompletionStage<*> -> r.thenCompose { v ->
            doDelay(delayable, start) { (delay, unit) ->
                val executor = CompletableFuture.delayedExecutor(delay, unit, executor)
                CompletableFuture.supplyAsync({ v }, executor)
            } ?: CompletableFuture.completedFuture(v)
        }

        else -> doDelay(delayable, start) { (delay, unit) ->
            CompletableFuture.supplyAsync({ r }, CompletableFuture.delayedExecutor(delay, unit, executor)).get()
        } ?: r
    }

    private fun <T> doDelay(delayable: Delayable, start: Long, func: (Pair<Long, TimeUnit>) -> T?) =
            apiDelayService.computeDelay(delayable.context, delayable.path, System.currentTimeMillis() - start)?.let(func)
}
