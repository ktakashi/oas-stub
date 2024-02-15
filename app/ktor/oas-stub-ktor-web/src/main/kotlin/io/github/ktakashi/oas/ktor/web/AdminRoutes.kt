package io.github.ktakashi.oas.ktor.web

import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.model.PluginType
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.application.call
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Optional
import org.koin.ktor.ext.inject

fun Route.apiInfoRouting() {
    val apiRegistrationService by inject<ApiRegistrationService>()
    get("/") {
        call.respond(HttpStatusCode.OK, apiRegistrationService.getAllNames())
    }
}

private inline fun <reified T: Any> Route.buildPropertyApis(noinline retriever: (ApiDefinitions) -> T?, noinline updator: (ApiDefinitions, T?) -> ApiDefinitions) {
    val apiRegistrationService by inject<ApiRegistrationService>()
    val save = { context: String, value: T? ->
        apiRegistrationService.getApiDefinitions(context).map { def: ApiDefinitions -> updator(def, value) }
            .map { def -> if (apiRegistrationService.saveApiDefinitions(context, def)) def else null }
    }

    get {
        val context = call.parameters["context"] ?: return@get call.respond(HttpStatusCode.NotFound)
        val v = apiRegistrationService.getApiDefinitions(context).map(retriever)
        if (v.isPresent) {
            call.respond(HttpStatusCode.OK, v.get())
        } else {
            call.respond<HttpStatusCode>(HttpStatusCode.NotFound)
        }
    }
    put {
        val context = call.parameters["context"] ?: return@put call.respond(HttpStatusCode.NotFound)
        val value = call.receive<T>()
        val v = save(context, value)
        if (v.isPresent) {
            call.respond<ApiDefinitions>(HttpStatusCode.OK, v.get())
        } else {
            call.respond<HttpStatusCode>(HttpStatusCode.NotFound)
        }
    }
    delete {
        val context = call.parameters["context"] ?: return@delete call.respond(HttpStatusCode.NotFound)
        val v = save(context, null)
        call.respond<HttpStatusCode>(if (v.isPresent) {
            HttpStatusCode.NoContent
        } else {
            HttpStatusCode.NotFound
        })
    }
}

// TODO
typealias UriTemplate = String
private inline fun <reified T: Any> Route.buildConfigurationPropertyApis(withPut: Boolean, noinline retriever: (ApiConfiguration) -> T?, noinline updator: (ApiConfiguration?, T?) -> ApiConfiguration?) {
    val apiRegistrationService by inject<ApiRegistrationService>()
    val save = { context: String, api: UriTemplate, value: T? ->
        apiRegistrationService.saveConfigurationProperty(context, api) { config ->
            updator(config, value)
        }
    }
    get {
        val context = call.parameters["context"] ?: return@get call.respond(HttpStatusCode.NotFound)
        val api = call.request.queryParameters["api"] ?: return@get call.respond(HttpStatusCode.NotFound)
        val v = apiRegistrationService.getApiDefinitions(context).map { def ->
            val decodedApi = URLDecoder.decode(api, StandardCharsets.UTF_8)
            def.configurations?.get(decodedApi)?.let(retriever)
        }
        if (v.isPresent) {
            call.respond(HttpStatusCode.OK, v.get())
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
    if (withPut) {
        put {
            val context = call.parameters["context"] ?: return@put call.respond(HttpStatusCode.NotFound)
            val api = call.request.queryParameters["api"] ?: return@put call.respond(HttpStatusCode.NotFound)
            val value = call.receive<T>()
            val v = save(context, api, value)
            if (v.isPresent) {
                call.respond(HttpStatusCode.OK, v.get())
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    delete {
        val context = call.parameters["context"] ?: return@delete call.respond(HttpStatusCode.NotFound)
        val api = call.request.queryParameters["api"] ?: return@delete call.respond(HttpStatusCode.NotFound)
        val v = save(context, api, null)
        call.respond(if (v.isPresent)
            HttpStatusCode.NoContent
        else
            HttpStatusCode.NotFound
        )
    }
}

private fun ApiRegistrationService.saveConfigurationProperty(context: String, api: UriTemplate, updator: (ApiConfiguration?) -> ApiConfiguration?): Optional<ApiDefinitions> = getApiDefinitions(context).map { def ->
    val decodedApi = URLDecoder.decode(api, StandardCharsets.UTF_8)
    updator(def.configurations?.get(decodedApi))?.let { config ->
        def.updateConfiguration(decodedApi, config)
    }
}.map { def ->
    if (saveApiDefinitions(context, def))
        def
    else
        null
}

private inline fun <reified T: Any> Route.buildConfigurationPropertyApis(noinline retriever: (ApiConfiguration) -> T?, noinline updator: (ApiConfiguration?, T?) -> ApiConfiguration?) {
    buildConfigurationPropertyApis(true, retriever, updator)
}
fun Route.contextRouting() {
    val apiRegistrationService by inject<ApiRegistrationService>()
    route("/{context}") {
        get {
            val context = call.parameters["context"] ?: return@get call.respond(HttpStatusCode.NotFound)
            val definitions = apiRegistrationService.getApiDefinitions(context)
            if (definitions.isPresent) {
                call.respond(definitions.get())
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        post {
            val context = call.parameters["context"] ?: return@post call.respond(HttpStatusCode.NotFound)
            val contentType = call.request.contentType()
            when {
                contentType.match(ContentType.Application.Json) -> if (apiRegistrationService.saveApiDefinitions(context, call.receive())) {
                    with(call) {
                        headersOf(HttpHeaders.Location, "/${context}")
                        respond(HttpStatusCode.Created)
                    }
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
                contentType.match(ContentType.Text.Plain) || contentType.match(ContentType.Application.OctetStream) -> ApiDefinitions(call.receive<String>()).let { definitions ->
                    if (apiRegistrationService.saveApiDefinitions(context, definitions)) {
                        with(call) {
                            headersOf(HttpHeaders.Location, "/${context}")
                            respond(HttpStatusCode.Created)
                        }
                    } else {
                        call.respond(HttpStatusCode.UnprocessableEntity)
                    }
                }
                else -> call.respond(HttpStatusCode.NotAcceptable)
            }
        }
        delete {
            val context = call.parameters["context"] ?: return@delete call.respond(HttpStatusCode.NotFound)
            if (apiRegistrationService.deleteApiDefinitions(context)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        route("/options") {
            buildPropertyApis(ApiDefinitions::options) { def, value -> def.updateOptions(value) }
        }
        route("/configurations") {
            buildPropertyApis(ApiDefinitions::configurations) { def, value -> def.updateConfigurations(value) }
        }
        route("/headers") {
            buildPropertyApis(ApiDefinitions::headers) { def, value -> def.updateHeaders(value) }
        }
        route("/data") {
            buildPropertyApis(ApiDefinitions::data) { def, value -> def.updateData(value) }
        }
        route("/delay") {
            buildPropertyApis(ApiDefinitions::delay) { def, value -> def.updateDelay(value) }
        }

        route("/configurations/plugins") {
            buildConfigurationPropertyApis(false, ApiConfiguration::plugin) { config, v ->
                config?.updatePlugin(null)
            }
            put("/groovy") {
                val context = call.parameters["context"] ?: return@put call.respond(HttpStatusCode.NotFound)
                val api = call.request.queryParameters["api"] ?: return@put call.respond(HttpStatusCode.NotFound)
                val value = call.receive<String>()
                val v = apiRegistrationService.saveConfigurationProperty(context, api) { config ->
                    PluginDefinition(type = PluginType.GROOVY, script = value).let {
                        config?.updatePlugin(it) ?: ApiConfiguration(plugin = it)
                    }
                }
                if (v.isPresent) {
                    call.respond(HttpStatusCode.OK, v.get())
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        route("/configurations/headers") {
            buildConfigurationPropertyApis(ApiConfiguration::headers) { config, v ->
                config?.updateHeaders(v) ?: v?.let { ApiConfiguration(headers = v) }
            }
        }
        route("/configurations/options") {
            buildConfigurationPropertyApis(ApiConfiguration::options) { config, v ->
                config?.updateOptions(v) ?: v?.let { ApiConfiguration(options = v) }
            }
        }
        route("/configurations/data") {
            buildConfigurationPropertyApis(ApiConfiguration::data) { config, v ->
                config?.updateData(v) ?: v?.let { ApiConfiguration(data = v) }
            }
        }
        route("/configurations/delay") {
            buildConfigurationPropertyApis(ApiConfiguration::delay) { config, v ->
                config?.updateDelay(v) ?: v?.let { ApiConfiguration(delay = v) }
            }
        }
    }
}