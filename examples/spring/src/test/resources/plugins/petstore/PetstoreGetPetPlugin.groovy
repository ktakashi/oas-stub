package plugins.petstore


import io.github.ktakashi.oas.plugin.apis.ApiPlugin
import io.github.ktakashi.oas.plugin.apis.PluginContext
import io.github.ktakashi.oas.plugin.apis.ResponseContext

import java.nio.charset.StandardCharsets

class PetstoreGetPetPlugin implements ApiPlugin {
    @Override
    ResponseContext customize(PluginContext pluginContext) {
        return pluginContext.responseContext.mutate()
                .content("""{"id": 1,"name": "Tama","tag": "Cat"}""".getBytes(StandardCharsets.UTF_8))
                .build()
    }
}
