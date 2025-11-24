package plugins

import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.api.plugin.ApiPlugin
import io.github.ktakashi.oas.api.plugin.PluginContext
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ArrayNode

class PetStoreGetPetsPlugin implements ApiPlugin {
    def objectMapper = new ObjectMapper()

    @Override
    ResponseContext customize(PluginContext pluginContext) {
        def request = pluginContext.requestContext
        def response = pluginContext.responseContext
        if (request.method != "GET") {
            return response.mutate().status(405).build()
        }

        if (response.status == 200) {
            def node = objectMapper.readTree(response.getContent().get())
            if (node.isArray()) {
                def array = node as ArrayNode
                def pet = array.get(0)
                array.removeAll()
                array.add(pet)
                def content = objectMapper.writeValueAsBytes(array)
                return response.mutate().content(content).build()
            }
        }
        return response
    }
}
