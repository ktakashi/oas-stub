package io.github.ktakashi.oas.server.config

import io.github.ktakashi.oas.engine.models.ApiDefinitionsMerger
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.ApiOptions
import io.github.ktakashi.oas.server.options.OasStubStubOptions

class ServerApiDefinitionsMerger(private val options: OasStubStubOptions): ApiDefinitionsMerger {
    private val baseDefinitions: ApiDefinitions? = options.toApiDefinitions()
    override fun mergeApiDefinitions(definitions: ApiDefinitions): ApiDefinitions = baseDefinitions?.let {
        if (definitions.options == null) {
            definitions.updateOptions(baseDefinitions.options)
        } else {
            definitions
        }
    } ?: definitions

    private fun OasStubStubOptions.toApiDefinitions() = options.toApiOptions()?.let {
        ApiDefinitions(options = it)
    }

    private fun OasStubStubOptions.toApiOptions() = if (this.enableAdmin && (this.enableMetrics || this.enableRecord)) {
        ApiOptions(shouldMonitor = this.enableMetrics, shouldRecord = this.enableRecord)
    } else {
        null
    }
}