package plugins

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.models.Profile
import io.github.ktakashi.oas.plugin.apis.ApiPlugin
import io.github.ktakashi.oas.plugin.apis.PluginContext
import io.github.ktakashi.oas.plugin.apis.ResponseContext

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
