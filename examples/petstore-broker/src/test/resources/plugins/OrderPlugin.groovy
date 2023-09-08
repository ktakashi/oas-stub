package plugins

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.plugin.apis.ApiPlugin
import io.github.ktakashi.oas.plugin.apis.PluginContext
import io.github.ktakashi.oas.plugin.apis.ResponseContext
import oas.example.petstore.broker.models.order.NewOrder
import oas.example.petstore.broker.models.order.Order

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
        def order = new Order("random-id", message.reference)
        return pluginContext.responseContext
                .mutate()
                .content(objectMapper.writeValueAsBytes(order))
                .build()
    }
}
