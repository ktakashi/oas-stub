package io.github.ktakashi.oas.engine.models

import io.github.ktakashi.oas.engine.paths.findMatchingPathValue
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.MergeableApiConfig

object ModelPropertyUtils {
    @JvmStatic
    fun <R : MergeableApiConfig<R>> mergeProperty(path: String, d: ApiDefinitions, propertyRetriever: (ApiCommonConfigurations<*>) -> R?) =
            d.configurations?.let {
                findMatchingPathValue(path, it)
                        .map { def -> propertyRetriever(def) }
                        .map { o -> propertyRetriever(d)?.let { o?.merge(it) } ?: o }
                        .orElseGet { propertyRetriever(d) }
            } ?: propertyRetriever(d)
}
