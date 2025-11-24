package plugins

import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.api.plugin.ApiPlugin
import io.github.ktakashi.oas.api.plugin.PluginContext
import oas.example.petstore.broker.models.order.NewOrder
import oas.example.petstore.broker.models.order.Order
import tools.jackson.databind.ObjectMapper

class OrderPlugin implements ApiPlugin {
    def objectMapper = new ObjectMapper()
    @Override
    ResponseContext customize(PluginContext pluginContext) {
        def message = pluginContext.requestContext.content.map { c ->
            objectMapper.readValue(c, NewOrder.class)
        }.orElse(null)
        if (message == null) {
            return pluginContext.responseContext
        }
        def order = new Order(pluginContext.getApiData("random-id", String.class).orElse("random-id"), message.reference)
        return pluginContext.responseContext
                .mutate()
                .content(objectMapper.writeValueAsBytes(order))
                .build()
    }
}
