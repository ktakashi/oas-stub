package oas.stub.plugins

import io.github.ktakashi.oas.api.plugin.ApiPlugin
import io.github.ktakashi.oas.api.plugin.PluginContext
import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.test.OasStubTestResources

class DefaultResponsePlugin implements ApiPlugin {
    @Override
    ResponseContext customize(PluginContext pluginContext) {
        return pluginContext.getApiData(pluginContext.requestContext.apiPath, OasStubTestResources.DefaultResponseModel.class)
                .or { pluginContext.getApiData("default", OasStubTestResources.DefaultResponseModel.class) }
                .map { it.toResponseContext(pluginContext.responseContext) }
                .orElseGet { pluginContext.responseContext }
    }
}
