package io.github.ktakashi.oas.servlets

import io.github.ktakashi.oas.OAS_API_PREFIX
import io.github.ktakashi.oas.engine.apis.ApiContext
import io.github.ktakashi.oas.engine.apis.ApiService
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

@WebServlet(urlPatterns = [ "$OAS_API_PREFIX/*" ], asyncSupported = true)
class OasDispatchServlet(private val apiService: ApiService): HttpServlet() {

    override fun service(req: HttpServletRequest, res: HttpServletResponse) {
        val asyncContext = req.startAsync()
        val listener = OasDispatchServletAsyncListener()
        asyncContext.addListener(listener)
        CompletableFuture.supplyAsync { apiService.getApiContext(req) }
                .thenComposeAsync { apiContext ->
                    if (listener.isCompleted) {
                        CompletableFuture.completedFuture(asyncContext)
                    } else if (apiContext.isPresent) {
                        processApi(asyncContext, req, apiContext.get())
                    } else {
                        res.status = 404
                        CompletableFuture.completedFuture(asyncContext)
                    }
                }
    }

    private fun processApi(asyncContext: AsyncContext, req: HttpServletRequest, apiContext: ApiContext): CompletableFuture<AsyncContext> {
        val response = asyncContext.response as HttpServletResponse
        return CompletableFuture.supplyAsync { apiService.executeApi(apiContext, req, response) }
                .thenApply { _ -> asyncContext }
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
//        val request = event.suppliedRequest as HttpServletRequest
        val response = event.suppliedResponse as HttpServletResponse
        response.status = 408
        event.asyncContext.complete()
    }

    override fun onError(event: AsyncEvent?) {
        // do nothing
    }

    override fun onStartAsync(event: AsyncEvent?) {
        // do nothing
    }

}
