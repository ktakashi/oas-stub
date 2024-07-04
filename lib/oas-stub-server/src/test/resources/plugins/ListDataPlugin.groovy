package plugins

import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.api.plugin.ApiPlugin
import io.github.ktakashi.oas.api.plugin.PluginContext
import org.jetbrains.annotations.NotNull

class ListDataPlugin implements ApiPlugin {
    @Override
    ResponseContext customize(@NotNull PluginContext pluginContext) {
        def list = pluginContext.getApiData("list", List.class)
        return pluginContext.responseContext.mutate()
                .content(pluginContext.objectWriter.writeValueAsBytes(list.get()))
                .build()
    }
}
