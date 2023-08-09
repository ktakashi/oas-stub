package io.github.ktakashi.oas.storage.apis

import io.github.ktakashi.oas.model.ApiDefinitions
import java.util.Optional

interface PersistentStorage {
    fun getApiDefinition(applicationName: String): Optional<ApiDefinitions>
    fun setApiDefinition(applicationName: String, apiDefinitions: ApiDefinitions)
}
