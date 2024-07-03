package io.github.ktakashi.oas.engine.models

import io.github.ktakashi.oas.model.ApiDefinitions

fun interface ApiDefinitionsMerger {
    /**
     * Merge the given definitions to the default definitions.
     *
     * If both definitions have the same field, then [definitions] will override it.
     */
    fun mergeApiDefinitions(definitions: ApiDefinitions): ApiDefinitions
}

internal fun ApiDefinitionsMerger?.merge(definitions: ApiDefinitions) = this?.mergeApiDefinitions(definitions) ?: definitions