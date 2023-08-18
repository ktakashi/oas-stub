package io.github.ktakashi.oas.cucumber.context

import io.restassured.http.Header
import io.restassured.response.Response

data class TestContext(var applicationUrl: String,
                       var prefix: String,
                       var apiDefinitionPath: String = "",
                       var apiName: String = "",
                       var headers: MutableList<Header> = mutableListOf(),
                       var response: Response? = null,
                       var responseTime: Long? = null)
