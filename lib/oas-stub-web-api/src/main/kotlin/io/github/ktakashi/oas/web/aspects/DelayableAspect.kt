package io.github.ktakashi.oas.web.aspects

import io.github.ktakashi.oas.engine.apis.ApiDelayService
import io.github.ktakashi.oas.web.annotations.Delayable
import java.time.Duration
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Aspect
class DelayableAspect(private val apiDelayService: ApiDelayService) {
    // Unfortunately, this AspectJ expression works only on Spring... :(
    @Around("@annotation(io.github.ktakashi.oas.web.annotations.Delayable) && @annotation(delayable)")
    fun delayAround(jointPoint: ProceedingJoinPoint, delayable: Delayable): Any? {
        val start = System.currentTimeMillis()
        val r = jointPoint.proceed()
        return proceedDelay(r, delayable, start)
    }

    fun proceedDelay(r: Any?, delayable: Delayable, start: Long): Any? = when (r) {
        is Mono<*> -> r.flatMap { v -> doDelay(delayable, start, v) }
        is Flux<*> -> r.flatMap { v -> doDelay(delayable, start, v) }
        is CompletionStage<*> -> r.thenCompose { v -> doDelay(delayable, start, v).toFuture() }
        else -> doDelay(delayable, start, r).toFuture().get()
    }

    private fun doDelay(delayable: Delayable, start: Long, v: Any?) = doDelay(delayable, start) { (delay, unit) -> Duration.of(delay, unit.toChronoUnit()) }
        .switchIfEmpty(Mono.just(Duration.ZERO))
        .flatMap { delay -> Mono.justOrEmpty(v).delayElement(delay) }

    private fun <T> doDelay(delayable: Delayable, start: Long, func: (Pair<Long, TimeUnit>) -> T) =
            apiDelayService.computeDelay(delayable.context, delayable.path, System.currentTimeMillis() - start).map(func)
}