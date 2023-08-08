package io.github.ktakashi.oas.storage.apis

import io.github.ktakashi.oas.model.ApiDefinition
import java.util.Optional

interface PersistentStorage {
    fun getApiDefinition(applicationName: String): Optional<ApiDefinition>
    fun setApiDefinition(applicationName: String, apiDefinition: ApiDefinition)
}
