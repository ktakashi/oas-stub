package plugins

import io.github.ktakashi.oas.api.plugin.ApiPlugin
import io.github.ktakashi.oas.api.plugin.PluginContext
import io.github.ktakashi.oas.api.http.ResponseContext

class PetStoreErrorPlugin implements ApiPlugin {
    @Override
    ResponseContext customize(PluginContext pluginContext) {
        throw new Exception("Expected exception, this shouldn't stop API execution")
    }
}
