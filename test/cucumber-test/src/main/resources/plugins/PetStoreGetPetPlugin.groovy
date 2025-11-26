package plugins

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import io.github.ktakashi.oas.api.plugin.ApiPlugin
import io.github.ktakashi.oas.api.plugin.PluginContext
import io.github.ktakashi.oas.api.http.ResponseContext

class PetStoreGetPetPlugin implements ApiPlugin {
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
            if (node.isObject()) {
                def object = node as ObjectNode
                object.replace("id", objectMapper.nodeFactory.numberNode(1))
                def content = objectMapper.writeValueAsBytes(object)
                return response.mutate().content(content).build()
            }
        }
        return response
    }
}
