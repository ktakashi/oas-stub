package io.github.ktakashi.oas.engine.models

import io.github.ktakashi.oas.engine.apis.ApiDefinitionsContext
import io.github.ktakashi.oas.engine.paths.findMatchingPathValue
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.MergeableApiConfig

// Merge root - path - method configuration
// the priority is reverse order (method is the strongest)
fun <R: MergeableApiConfig<R>> ApiDefinitionsContext.mergeProperty(propertyRetriever: (ApiCommonConfigurations<*>) -> R?): R? {
    return apiDefinitions.configurations?.let {
        findMatchingPathValue(this.apiPath, it)
            .map { config ->
                val methodConfig = config.methods?.get(this.method)?.let { methodConfiguration ->
                    propertyRetriever(methodConfiguration)
                }
                val mergedConfig = propertyRetriever(config)?.let { other -> methodConfig?.merge(other) ?: other } ?: methodConfig
                propertyRetriever(apiDefinitions)?.let { other -> mergedConfig?.merge(other) ?: other } ?: mergedConfig
            }.orElseGet { propertyRetriever(apiDefinitions) }
    } ?: propertyRetriever(apiDefinitions)
}
