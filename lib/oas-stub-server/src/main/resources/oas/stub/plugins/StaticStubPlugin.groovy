package oas.stub.plugins

import com.fasterxml.jackson.databind.ObjectWriter
import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.api.plugin.ApiPlugin
import io.github.ktakashi.oas.api.plugin.PluginContext

class StaticStubPlugin implements ApiPlugin {
    @Override
    ResponseContext customize(PluginContext pluginContext) {
        def status = pluginContext.getApiData("status", Integer.class)
        def contentType = pluginContext.getApiData("contentType", String.class)
        def response = pluginContext.getApiData("response", Object.class)
        def headers = pluginContext.getApiData("headers", Map.class)
        def responseContext = pluginContext.responseContext
        return responseContext.mutate()
                .status(status.orElse(responseContext.status))
                .contentType(contentType.orElseGet { responseContext.contentType.orElse(null) })
                .content(response.map { readResponse(it, pluginContext.objectWriter) }.orElse(null))
                .headers(headers.map {
                    Map<String, List<String>> r = new HashMap<String, List<String>>()
                    if (responseContext.headers != null) r.putAll(responseContext.headers)
                    it.forEach { String k, v ->
                        r.compute(k) { k2, v2 ->
                            if (v instanceof Collection) {

                            }
                        }
                    }
                    r
                }.orElseGet { responseContext.headers })
                .build()
    }

    static byte[] readResponse(Object response, ObjectWriter objectWriter) {
        if (response instanceof String) {
            String s = response
            if (s.startsWith("classpath:")) {
                return StaticStubPlugin.class.getResourceAsStream(s.substring("classpath:".length())).readAllBytes()
            } else {
                return s.getBytes()
            }
        } else {
            return objectWriter.writeValueAsBytes(response)
        }
    }
}
