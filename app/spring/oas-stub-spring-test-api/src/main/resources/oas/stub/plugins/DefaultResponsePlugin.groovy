package oas.stub.plugins

import io.github.ktakashi.oas.plugin.apis.ApiPlugin
import io.github.ktakashi.oas.plugin.apis.PluginContext
import io.github.ktakashi.oas.plugin.apis.ResponseContext
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
