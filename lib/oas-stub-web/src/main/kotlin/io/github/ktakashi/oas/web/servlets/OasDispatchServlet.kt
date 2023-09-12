package io.github.ktakashi.oas.web.servlets

import io.github.ktakashi.oas.engine.apis.ApiContext
import io.github.ktakashi.oas.engine.apis.ApiDelayService
import io.github.ktakashi.oas.engine.apis.ApiExecutionService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.engine.models.ModelPropertyUtils
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.ApiMetric
import io.github.ktakashi.oas.web.services.ExecutorProvider
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(OasDispatchServlet::class.java)
private const val EXECUTOR_NAME = "oasServlet"
private const val METRICS_START_KEY = "io.github.ktakashi.oas.metrics.start"
private const val API_CONTEXT_KEY = "io.github.ktakashi.oas.metrics.apiContext"

@Named @Singleton
class OasDispatchServlet
@Inject constructor(private val apiExecutionService: ApiExecutionService,
                    private val apiDelayService: ApiDelayService,
                    private val apiObserver: ApiObserver,
                    executorProvider: ExecutorProvider): HttpServlet() {
    private val executor = executorProvider.getExecutor(EXECUTOR_NAME)
    override fun service(req: HttpServletRequest, res: HttpServletResponse) {
        val asyncContext = req.startAsync()
        val listener = OasDispatchServletAsyncListener()
        val now = OffsetDateTime.now()
        asyncContext.addListener(listener)

        CompletableFuture.supplyAsync({ apiExecutionService.getApiContext(req) }, executor)
            .thenComposeAsync({ apiContext ->
                if (listener.isCompleted) {
                    CompletableFuture.completedFuture(asyncContext)
                } else {
                    apiContext.map { context ->
                        ModelPropertyUtils.mergeProperty(context.apiPath, context.apiDefinitions, ApiCommonConfigurations<*>::options)?.let { options ->
                            if (options.shouldMonitor == null || options.shouldMonitor == true) {
                                setAttributes(req, now, context)
                            }
                        } ?: setAttributes(req, now, context)
                        processApi(asyncContext, req, context)
                    }.orElseGet {
                        res.status = 404
                        CompletableFuture.completedFuture(asyncContext)
                    }
                }
            }, executor).exceptionally { e ->
                logger.error("Execution error: {}", e.message, e)
                if (!listener.isCompleted) {
                    res.status = 500
                    res.outputStream.write(e.message?.toByteArray() ?: byteArrayOf())
                }
                asyncContext
            }.whenComplete { context, e ->
                report(context, e)
                context.complete()
            }
    }

    private fun processApi(asyncContext: AsyncContext, req: HttpServletRequest, apiContext: ApiContext): CompletionStage<AsyncContext> {
        val response = asyncContext.response as HttpServletResponse
        return apiDelayService.delayFuture(apiContext, CompletableFuture.supplyAsync({ apiExecutionService.executeApi(apiContext, req, response) }, executor))
                .thenApply { responseContext ->
                    responseContext.emitResponse(response)
                    asyncContext
                }
    }

    private fun setAttributes(req: HttpServletRequest, now: OffsetDateTime, context: ApiContext) {
        req.setAttribute(METRICS_START_KEY, now)
        req.setAttribute(API_CONTEXT_KEY, context)
    }

    private fun report(context: AsyncContext, exception: Throwable?) {
        val request = context.request as HttpServletRequest
        val response = context.response as HttpServletResponse
        val end = OffsetDateTime.now()
        val start = request.getAttribute(METRICS_START_KEY) as OffsetDateTime?
        val apiContext = request.getAttribute(API_CONTEXT_KEY) as ApiContext?
        if (start != null && apiContext != null) {
            val metric = ApiMetric(start, Duration.between(start, end), request.method, response.status, exception)
            apiObserver.addApiMetric(apiContext.context, apiContext.apiPath, metric)
        }
    }
}

private class OasDispatchServletAsyncListener: AsyncListener {
    private val completed = AtomicBoolean(false)

    val isCompleted
        get() = completed.get()

    override fun onComplete(event: AsyncEvent) {
        // do nothing
    }

    override fun onTimeout(event: AsyncEvent) {
        completed.set(true)
        val response = event.suppliedResponse as HttpServletResponse
        response.status = 408 // request timeout
        event.asyncContext.complete()
    }

    override fun onError(event: AsyncEvent?) {
        // do nothing
    }

    override fun onStartAsync(event: AsyncEvent?) {
        // do nothing
    }

}
