package io.github.ktakashi.oas.engine.apis

import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.util.Optional

const val API_PATH_NAME_QUALIFIER = "ApiPathNameQualifier"

@Named @Singleton
class ApiPathService
@Inject constructor(@Named(API_PATH_NAME_QUALIFIER) private val prefix: String) {
    fun extractApplicationName(uri: String?): Optional<String> = uri?.let {
        val index = it.indexOf(prefix)
        if (index < 0) {
            Optional.empty()
        } else {
            val firstSlash = index + prefix.length + 1
            val lastSlash = it.indexOf('/', firstSlash)
            if (lastSlash < 0) {
                Optional.empty()
            } else {
                Optional.of(it.substring(firstSlash, lastSlash))
            }
        }
    } ?: Optional.empty()

    fun extractApiPath(context: String, uri: String): String {
        val contextPath = "$prefix/$context"
        val index = uri.indexOf(contextPath)
        return uri.substring(index + contextPath.length)
    }
}
