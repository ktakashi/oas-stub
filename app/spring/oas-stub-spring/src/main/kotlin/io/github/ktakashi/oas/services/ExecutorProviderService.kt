package io.github.ktakashi.oas.services

import io.github.ktakashi.oas.configuration.ExecutorsProperties
import io.github.ktakashi.oas.web.services.ExecutorProvider
import jakarta.annotation.PreDestroy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinWorkerThread
import org.springframework.stereotype.Service

@Service
class ExecutorProviderService(private val executorsProperties: ExecutorsProperties): ExecutorProvider {

    private val executors: ConcurrentHashMap<String, ExecutorService> = ConcurrentHashMap()
    override fun getExecutor(name: String) = executors.computeIfAbsent(name) { _ ->
        ForkJoinPool(executorsProperties.parallelism, ::WorkerThread, null, true)
    }

    @PreDestroy
    fun clear() {
        executors.forEach { (_, v) -> v.shutdown() }
    }
}

private class WorkerThread(pool: ForkJoinPool): ForkJoinWorkerThread(pool) {
    init {
        super.setContextClassLoader(WorkerThread::class.java.classLoader)
    }
}
