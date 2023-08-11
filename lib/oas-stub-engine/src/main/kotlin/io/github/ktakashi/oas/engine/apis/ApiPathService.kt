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


    fun <T : Any> findMatchingPath(path: String, paths: Map<String, T>): Optional<String> = if (paths.containsKey(path)) {
        Optional.ofNullable(path)
    } else {
        Optional.ofNullable(paths.entries.firstOrNull { (k, _) -> pathMatches(k, path) }?.key)
    }

    fun <T : Any> findMatchingPathValue(path: String, paths: Map<String, T>): Optional<T> = findMatchingPath(path, paths)
            .map { p -> paths[p] }

    private fun pathMatches(template: String, path: String): Boolean {
        var i = 0
        var j = 0
        do {
            val (result, ni, nj) = matchSegment(template, path, i, j)
            if (!result) {
                return false
            }
            i = ni
            j = nj
            if (i < 0 || j < 0) {
                break
            }
        } while (i < template.length && j < path.length)

        return (i < 0 && j < 0 && i == j) || i == template.length && j == path.length
    }

    // checking segment, means inbetween '/'s, e.g. /foo/
    private fun matchSegment(template: String, path: String, i: Int, j: Int): Triple<Boolean, Int, Int> {
        var ts = template.indexOf('/', i)
        var ps = path.indexOf('/', j)
        if (ts < 0 || ps < 0) {
            return Triple(false, -1, -1) // unmatched segment number
        }
        ts++
        ps++
        if (ts == template.length || ps == path.length) {
            return Triple((ts == template.length && ps == path.length), ts, ps)
        }

        if (template[ts] == '{') {
            val k = template.indexOf('}', ts)
            if (k > 0 && ((k+1 < template.length && template[k+1] == '/') || k < template.length)) {
                return Triple(true, template.indexOf('/', ts) - 1, path.indexOf('/', ps) - 1)
            }
        }

        // literal match
        while (ts < template.length && ps < path.length) {
            val c0 = template[ts]
            val c1 = path[ps]
            if (c0 != c1) {
                return Triple(false, -1, -1)
            }
            if (c0 == '/') {
                return Triple(true, ts, ps)
            }
            ts++
            ps++
        }
        // no trailing '/'
        return Triple(true, ts, ps)
    }
}
