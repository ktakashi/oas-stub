package io.github.ktakashi.oas.servlets

import io.github.ktakashi.oas.engine.apis.ApiContext
import io.github.ktakashi.oas.engine.apis.ApiDelayService
import io.github.ktakashi.oas.engine.apis.ApiExecutionService
import io.github.ktakashi.oas.services.ExecutorProviderService
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

private val logger = LoggerFactory.getLogger(OasDispatchServlet::class.java)
private const val EXECUTOR_NAME = "oasServlet"

@Component
class OasDispatchServlet(private val apiExecutionService: ApiExecutionService,
                         private val apiDelayService: ApiDelayService,
                         executorProviderService: ExecutorProviderService): HttpServlet() {
    private val executor = executorProviderService.getExecutor(EXECUTOR_NAME)
    override fun service(req: HttpServletRequest, res: HttpServletResponse) {
        val asyncContext = req.startAsync()
        val listener = OasDispatchServletAsyncListener()
        asyncContext.addListener(listener)

        CompletableFuture.supplyAsync({ apiExecutionService.getApiContext(req) }, executor)
                .thenComposeAsync({ apiContext ->
                    if (listener.isCompleted) {
                        CompletableFuture.completedFuture(asyncContext)
                    } else if (apiContext.isPresent) {
                        processApi(asyncContext, req, apiContext.get())
                    } else {
                        res.status = HttpStatus.NOT_FOUND.value()
                        CompletableFuture.completedFuture(asyncContext)
                    }
                }, executor).exceptionally { e ->
                    logger.error("Execution error: {}", e.message, e)
                    if (!listener.isCompleted) {
                        res.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
                        res.outputStream.write(e.message?.toByteArray() ?: byteArrayOf())
                    }
                    asyncContext
                }.whenComplete { context, _ -> context.complete() }
    }

    private fun processApi(asyncContext: AsyncContext, req: HttpServletRequest, apiContext: ApiContext): CompletionStage<AsyncContext> {
        val response = asyncContext.response as HttpServletResponse
        return apiDelayService.delayFuture(apiContext, CompletableFuture.supplyAsync({ apiExecutionService.executeApi(apiContext, req, response) }, executor))
                .thenApply { responseContext ->
                    responseContext.emitResponse(response)
                    asyncContext
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
        response.status = HttpStatus.REQUEST_TIMEOUT.value()
        event.asyncContext.complete()
    }

    override fun onError(event: AsyncEvent?) {
        // do nothing
    }

    override fun onStartAsync(event: AsyncEvent?) {
        // do nothing
    }

}
