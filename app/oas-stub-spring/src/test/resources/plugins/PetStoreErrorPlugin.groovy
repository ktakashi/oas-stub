package plugins

import io.github.ktakashi.oas.plugin.apis.ApiPlugin
import io.github.ktakashi.oas.plugin.apis.PluginContext
import io.github.ktakashi.oas.plugin.apis.ResponseContext
import org.jetbrains.annotations.NotNull

class PetStoreErrorPlugin implements ApiPlugin {
    @Override
    ResponseContext customize(@NotNull PluginContext pluginContext) {
        throw new Exception("Expected exception, this shouldn't stop API execution")
    }
}
