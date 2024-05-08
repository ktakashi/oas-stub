package plugins

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.api.plugin.ApiPlugin
import io.github.ktakashi.oas.api.plugin.PluginContext
import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.test.models.Profile

class TestApiGetProfileApiDataAwarePlugin implements ApiPlugin {
    def objectMapper = new ObjectMapper()
    @Override
    ResponseContext customize(PluginContext pluginContext) {
        def response = pluginContext.responseContext
        return pluginContext.getApiData("profile", Profile.class)
                .or { pluginContext.getApiData("profile2", Map.class) }
                .map { profile -> response.mutate().content(objectMapper.writeValueAsBytes(profile)).build() }
                .orElse(response)
    }
}
