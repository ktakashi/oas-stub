package io.github.ktakashi.oas.engine.apis

import java.util.Optional

class ApiPathService(private val prefix: String) {
    fun extractApiNameAndPath(uri: String?): Optional<Pair<String, String>> = uri?.let {
        val index = it.indexOf(prefix)
        if (index < 0) {
            Optional.empty()
        } else {
            val firstSlash = index + prefix.length + 1
            val lastSlash = it.indexOf('/', firstSlash)
            if (lastSlash < 0) {
                Optional.empty()
            } else {
                val context = it.substring(firstSlash, lastSlash)
                Optional.of(context to extractApiPath(context, uri))
            }
        }
    } ?: Optional.empty()

    private fun extractApiPath(context: String, uri: String): String {
        val contextPath = "$prefix/$context"
        val index = uri.indexOf(contextPath)
        return uri.substring(index + contextPath.length)
    }
}
