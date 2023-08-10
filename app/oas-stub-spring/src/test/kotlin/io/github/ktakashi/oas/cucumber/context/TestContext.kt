package io.github.ktakashi.oas.cucumber.context

import io.restassured.response.Response

data class TestContext(var applicationUrl: String,
                       var apiDefinitionPath: String = "",
                       var apiName: String = "",
                       var response: Response? = null)
