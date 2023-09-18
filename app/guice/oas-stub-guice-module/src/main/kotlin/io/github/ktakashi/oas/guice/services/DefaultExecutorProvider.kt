package io.github.ktakashi.oas.guice.services

import io.github.ktakashi.oas.guice.configurations.OasStubConfiguration
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceConfiguration
import io.github.ktakashi.oas.web.services.ExecutorProvider
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinWorkerThread

@Named @Singleton
class DefaultExecutorProvider
@Inject constructor(private val oasStubGuiceConfiguration: OasStubGuiceConfiguration): ExecutorProvider {

    private val executors: ConcurrentHashMap<String, ExecutorService> = ConcurrentHashMap()
    override fun getExecutor(name: String) = executors.computeIfAbsent(name) { _ ->
        ForkJoinPool(oasStubGuiceConfiguration.oasStubConfiguration.parallelism, ::WorkerThread, null, true)
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
