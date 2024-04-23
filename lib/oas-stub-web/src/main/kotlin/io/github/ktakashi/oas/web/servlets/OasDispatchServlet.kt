package io.github.ktakashi.oas.web.servlets

import io.github.ktakashi.oas.engine.apis.ApiContext
import io.github.ktakashi.oas.engine.apis.ApiDelayService
import io.github.ktakashi.oas.engine.apis.ApiExecutionService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.engine.models.ModelPropertyUtils
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.ApiMetric
import io.github.ktakashi.oas.plugin.apis.HttpRequest
import io.github.ktakashi.oas.plugin.apis.HttpResponse
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
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpCookie
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Collections
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
        val request = ServletHttpRequest(req)
        CompletableFuture.supplyAsync({ apiExecutionService.getApiContext(request) }, executor)
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
                        processApi(asyncContext, request, context)
                    }.orElseGet {
                        res.status = 404
                        CompletableFuture.completedFuture(asyncContext)
                    }
                }
            }, executor)
            .exceptionally { e ->
                logger.error("Execution error: {}", e.message, e)
                if (!listener.isCompleted) {
                    res.status = 500
                    res.outputStream.write(e.message?.toByteArray() ?: byteArrayOf())
                }
                asyncContext
            }
            .handleAsync({ context, e ->
                report(context, e)
                context
            }, executor)
            .whenComplete { context, _ ->
                context.complete()
            }
    }

    private fun processApi(asyncContext: AsyncContext, req: HttpRequest, apiContext: ApiContext): CompletionStage<AsyncContext> {
        val response = ServletHttpResponse(asyncContext.response as HttpServletResponse)
        return apiDelayService.delayFuture(apiContext, CompletableFuture.supplyAsync({ apiExecutionService.executeApi(apiContext, req, response) }, executor))
                .thenApplyAsync({ responseContext ->
                    logger.debug("Response -> {}", responseContext)
                    responseContext.emitResponse(response)
                    asyncContext
                }, executor)
    }

    private fun setAttributes(req: HttpServletRequest, now: OffsetDateTime, context: ApiContext) {
        req.setAttribute(METRICS_START_KEY, now)
        req.setAttribute(API_CONTEXT_KEY, context)
    }

    private fun report(context: AsyncContext, exception: Throwable?) {
        try {
            val request = context.request as HttpServletRequest
            val response = context.response as HttpServletResponse
            val end = OffsetDateTime.now()
            val start = request.getAttribute(METRICS_START_KEY) as OffsetDateTime?
            val apiContext = request.getAttribute(API_CONTEXT_KEY) as ApiContext?
            if (start != null && apiContext != null) {
                val metric = ApiMetric(start, Duration.between(start, end), apiContext.apiPath, request.method, response.status, exception)
                apiObserver.addApiMetric(apiContext.context, apiContext.apiPath, metric)
            }
        } catch (e: Exception) {
            logger.warn("Failed to add metric", e)
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

class ServletHttpRequest(private val request: HttpServletRequest): HttpRequest {
    private val internalCookies = request.cookies?.map { c ->
        HttpCookie(c.name, c.value).apply {
            domain = c.domain
            path = c.path
            isHttpOnly = c.isHttpOnly
            secure = c.secure
        }
    } ?: listOf()
    override val requestURI: String
        get() = request.requestURI
    override val method: String
        get() = request.method
    override val contentType: String
        get() = request.contentType
    override val cookies: List<HttpCookie>
        get() = internalCookies
    override val queryString: String
        get() = request.queryString
    override val inputStream: InputStream
        get() = request.inputStream

    override fun getHeader(name: String): String? = request.getHeader(name)

    override fun getHeaders(name: String): List<String> = Collections.list(request.getHeaders(name))

    override val headerNames: Collection<String>
        get() = Collections.list(request.headerNames)
}

class ServletHttpResponse(private val response: HttpServletResponse): HttpResponse {
    override var status: Int
        get() = response.status
        set(value) { response.status = value }
    override var contentType: String
        get() = response.contentType
        set(value) { response.contentType = value }

    override fun addHeader(name: String, value: String) = response.addHeader(name, value)

    override val outputStream: OutputStream
        get() = response.outputStream

}