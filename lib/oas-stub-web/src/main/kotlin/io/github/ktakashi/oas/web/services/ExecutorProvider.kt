package io.github.ktakashi.oas.web.services

import java.util.concurrent.ExecutorService

fun interface ExecutorProvider {
    fun getExecutor(name: String): ExecutorService
}
