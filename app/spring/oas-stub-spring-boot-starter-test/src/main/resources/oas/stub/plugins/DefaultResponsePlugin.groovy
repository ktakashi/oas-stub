package oas.stub.plugins

import io.github.ktakashi.oas.plugin.apis.ApiPlugin
import io.github.ktakashi.oas.plugin.apis.PluginContext
import io.github.ktakashi.oas.plugin.apis.ResponseContext
import org.springframework.core.io.ClassPathResource

import java.nio.charset.StandardCharsets

class DefaultResponsePlugin implements ApiPlugin {
    @Override
    ResponseContext customize(PluginContext pluginContext) {
        return pluginContext.getApiData(pluginContext.requestContext.apiPath, DefaultResponseModel.class)
                .or { pluginContext.getApiData("default", DefaultResponseModel.class) }
                .map { it.toResponseContext(pluginContext.responseContext) }
                .orElseGet { pluginContext.responseContext }
    }

    static class DefaultResponseModel {
        Integer status
        Map<String, List<String>> headers
        String response

        ResponseContext toResponseContext(ResponseContext original) {
            SortedMap<String, List<String>> header = new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
            header.putAll(original.headers)
            if (headers != null) {
                header.putAll(headers)
            }
            return original.mutate()
                    .status(status != null? status : original.status)
                    .headers(header)
                    .content(response != null? readResponse(response): original.content.orElse(null))
                    .build()
        }

        static byte[] readResponse(String response) {
            if (response.startsWith("classpath:")) {
                def resource = new ClassPathResource(response.substring("classpath:".length()))
                return resource.inputStream.readAllBytes()
            }
            return response.getBytes(StandardCharsets.UTF_8)
        }

    }
}
