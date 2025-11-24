package plugins

import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.api.plugin.ApiPlugin
import io.github.ktakashi.oas.api.plugin.PluginContext
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode

class PetStoreGetPetPlugin implements ApiPlugin {
    def objectMapper = new JsonMapper()

    @Override
    ResponseContext customize(PluginContext pluginContext) {
        def context = pluginContext.responseContext
        if (context.status == 200) {
            def node = objectMapper.readTree(context.getContent().get())
            if (node.isObject()) {
                def object = node as ObjectNode
                object.replace("id", objectMapper.nodeFactory.numberNode(1))
                def content = objectMapper.writeValueAsBytes(object)
                return context.mutate().content(content).build()
            }
        }
        return context
    }
}
