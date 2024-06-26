package plugins

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.ktakashi.oas.api.plugin.ApiPlugin
import io.github.ktakashi.oas.api.plugin.PluginContext
import io.github.ktakashi.oas.api.http.ResponseContext

class PetStoreGetPetPlugin implements ApiPlugin {
    def objectMapper = new ObjectMapper()

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
