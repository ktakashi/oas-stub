package plugins

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.github.ktakashi.oas.plugin.apis.ApiPlugin
import io.github.ktakashi.oas.plugin.apis.PluginContext
import io.github.ktakashi.oas.plugin.apis.ResponseContext

class PetStoreGetPetsPlugin implements ApiPlugin {
    def objectMapper = new ObjectMapper()

    @Override
    ResponseContext customize(PluginContext pluginContext) {
        def context = pluginContext.responseContext
        if (context.status == 200) {
            def node = objectMapper.readTree(context.getContent().get())
            if (node.isArray()) {
                def array = node as ArrayNode
                def pet = array.get(0)
                array.removeAll()
                array.add(pet)
                def content = objectMapper.writeValueAsBytes(array)
                return context.mutate().content(content).build()
            }
        }
        return context
    }
}
