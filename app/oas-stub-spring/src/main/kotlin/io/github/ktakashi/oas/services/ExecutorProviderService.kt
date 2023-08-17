package io.github.ktakashi.oas.services

import io.github.ktakashi.oas.configuration.ExecutorsProperties
import jakarta.annotation.PreDestroy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.springframework.stereotype.Service

@Service
class ExecutorProviderService(private val executorsProperties: ExecutorsProperties) {

    private val executors: ConcurrentHashMap<String, ExecutorService> = ConcurrentHashMap()
    fun getExecutor(name: String) = executors.computeIfAbsent(name) { _ ->
        Executors.newWorkStealingPool(executorsProperties.parallelism)
    }
    @PreDestroy
    fun clear() {
        executors.forEach { (_, v) -> v.shutdown() }
    }

}
