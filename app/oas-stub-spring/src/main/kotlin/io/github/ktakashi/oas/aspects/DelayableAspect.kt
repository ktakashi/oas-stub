package io.github.ktakashi.oas.aspects

import io.github.ktakashi.oas.annotations.Delayable
import io.github.ktakashi.oas.engine.apis.ApiDelayService
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Aspect
@Component
class DelayableAspect(private val apiDelayService: ApiDelayService) {
    @Around("@annotation(io.github.ktakashi.oas.annotations.Delayable) && @annotation(delayable)")
    fun delayMono(jointPoint: ProceedingJoinPoint, delayable: Delayable): Any {
        val start = System.currentTimeMillis()
        return when (val r = jointPoint.proceed()) {
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
                    val executor = CompletableFuture.delayedExecutor(delay, unit)
                    CompletableFuture.supplyAsync({ v }, executor)
                } ?: CompletableFuture.completedFuture(v)
            }
            else -> doDelay(delayable, start) { (delay, unit) ->
                CompletableFuture.supplyAsync({ r }, CompletableFuture.delayedExecutor(delay, unit)).get()
            } ?: r
        }
    }

    private fun <T> doDelay(delayable: Delayable, start: Long, func: (Pair<Long, TimeUnit>) -> T?) =
            apiDelayService.computeDelay(delayable.context, delayable.path, System.currentTimeMillis() - start)?.let(func)
}
