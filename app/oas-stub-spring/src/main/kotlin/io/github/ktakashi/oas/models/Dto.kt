package io.github.ktakashi.oas.models

import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiOptions
import io.github.ktakashi.oas.model.Headers
import io.github.ktakashi.oas.model.PluginType

data class CreateApiRequest(val api: String, // Raw Swagger or OAS definition
                            val apiConfigurations: Map<String, ApiConfiguration> = mapOf(),
                            val headers: Headers = Headers(),
                            val apiOptions: ApiOptions = ApiOptions(),
                            val apiData: Map<String, Any> = mapOf())

data class PutPluginRequest(val type: PluginType, val script: String)
