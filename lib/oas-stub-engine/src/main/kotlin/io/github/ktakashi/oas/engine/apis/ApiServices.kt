package io.github.ktakashi.oas.engine.apis

import io.github.ktakashi.oas.api.http.HttpRequest
import io.github.ktakashi.oas.api.http.HttpResponse
import io.github.ktakashi.oas.api.http.RequestContext
import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.engine.models.ModelPropertyUtils
import io.github.ktakashi.oas.engine.parsers.ParsingService
import io.github.ktakashi.oas.engine.paths.findMatchingPath
import io.github.ktakashi.oas.engine.paths.findMatchingPathValue
import io.github.ktakashi.oas.engine.plugins.PluginService
import io.github.ktakashi.oas.engine.storages.StorageService
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.ApiOptions
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.SpecVersion
import java.io.IOException
import java.io.InputStream
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URI
import java.time.Duration
import java.util.Optional
import java.util.TreeMap
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.function.TupleUtils

data class ApiContext(val context: String, val apiPath: String, val method: String, val openApi: OpenAPI, val apiDefinitions: ApiDefinitions)

class ApiException(val requestContext: ApiContextAwareRequestContext, val responseContext: ResponseContext): Exception()


fun interface ApiContextService {
    /**
     * Retrieves [ApiContext] from the [request]
     *
     * If context is not found, then the result would be empty
     */
    fun getApiContext(request: HttpRequest): Mono<ApiContext>
}

/**
 * The API execution service.
 *
 * The service is targeted to use in an HTTP servlet, the below describes how to
 * execute an API.
 *
 * ```java
 * public class MyServlet extends HttpServlet {
 *     // The service is meant to be used via DI, if you want to
 *     // instantiate manually, use DefaultApiService
 *     private ApiExecutionService apiExecutionService = // initiation
 *
 *     @Override
 *     protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
 *         try {
 *             apiExecutionService.getApiContext(req)
 *               .ifPresentOrElse(context -> apiExecutionService.executeApi(context, req, resp),
 *                   () -> resp.setStatus(404));
 *         } catch (Exception e) {
 *             resp.setStatus(500);
 *         }
 *     }
 * }
 * ```
 */
interface ApiExecutionService: ApiContextService {
    /**
     * Executes the given [apiContext]
     *
     * The response is intact after the call, so users must call [ResponseContext.emitResponse]
     */
    fun executeApi(apiContext: ApiContext, request: HttpRequest, response: HttpResponse): Mono<ResponseContext>
}

/**
 * The API registration interface
 *
 * This interface should be used to implement admin APIs, e.g. REST controllers.
 */
interface ApiRegistrationService {
    /**
     * Retrieves the [ApiDefinitions] associated to the [name]
     */
    fun getApiDefinitions(name: String): Mono<ApiDefinitions>

    /**
     * Saves the [apiDefinitions] with association to the [name]
     */
    fun saveApiDefinitions(name: String, apiDefinitions: ApiDefinitions): Mono<ApiDefinitions>

    /**
     * Deletes API definition from the [name]
     */
    fun deleteApiDefinitions(name: String): Mono<Boolean>

    /**
     * Retrieves all the registered API names
     */
    fun getAllNames(): Flux<String>

    fun validPath(definitions: ApiDefinitions, path: String): Mono<String>
}

private val logger = LoggerFactory.getLogger(ApiExecutionService::class.java)

class DefaultApiRegistrationService(private val storageService: StorageService,
                                    private val parsingService: ParsingService,) : ApiRegistrationService {
    override fun getApiDefinitions(name: String): Mono<ApiDefinitions> = storageService.getApiDefinitions(name)
    override fun saveApiDefinitions(name: String, apiDefinitions: ApiDefinitions): Mono<ApiDefinitions> = apiDefinitions.specification?.let { spec ->
        parsingService.parse(spec, false).flatMap { openApi ->
            when (openApi.specVersion) {
                // V31 isn't one-to-one mapping, so keep the original one
                SpecVersion.V31 -> Mono.just(apiDefinitions)
                else -> parsingService.toYaml(openApi).map(apiDefinitions::updateSpecification)
            }.map { stripInvalidConfiguration(openApi, it) }
        }.flatMap { def -> Mono.justOrEmpty(storageService.saveApiDefinitions(name, def)) }
    } ?: Mono.defer { Mono.justOrEmpty(storageService.saveApiDefinitions(name, apiDefinitions)) }

    override fun deleteApiDefinitions(name: String): Mono<Boolean> = Mono.defer { Mono.just(storageService.deleteApiDefinitions(name)) }

    override fun getAllNames(): Flux<String> = storageService.getApiNames()

    override fun validPath(definitions: ApiDefinitions, path: String): Mono<String> = definitions.specification?.let { spec ->
        parsingService.parse(spec).flatMap { openApi ->
            Mono.justOrEmpty(adjustBasePath(path, openApi).flatMap { p -> findMatchingPath(p, openApi.paths.keys) })
                .map { path }
        }
    } ?: Mono.just(path)

    private fun stripInvalidConfiguration(openApi: OpenAPI, apiDefinitions: ApiDefinitions): ApiDefinitions {
        val newConfig = apiDefinitions.configurations?.filterKeys { path ->
            adjustBasePath(path, openApi).map { p -> findMatchingPath(p, openApi.paths.keys) }.isPresent
        }
        return apiDefinitions.updateConfigurations(newConfig)
    }

}

class DefaultApiService(private val storageService: StorageService,
                        private val apiPathService: ApiPathService,
                        private val apiResultProvider: ApiResultProvider,
                        private val apiFailureService: ApiFailureService,
                        private val pluginService: PluginService): ApiExecutionService {
    override fun getApiContext(request: HttpRequest): Mono<ApiContext> =
        apiPathService.extractApiNameAndPath(request.requestURI).map { (context, api) ->
            storageService.getApiDefinitions(context).flatMap { apiDefinitions ->
                storageService.getOpenApi(context)
                    .map { def ->
                        ApiContext(
                            openApi = def,
                            context = context,
                            apiPath = api,
                            method = request.method,
                            apiDefinitions = apiDefinitions
                        )
                    }
            }
        }.orElseGet { Mono.empty() }

    override fun executeApi(apiContext: ApiContext, request: HttpRequest, response: HttpResponse): Mono<ResponseContext> =
        makeRequestContext(apiContext, apiContext.apiPath, request, response)
            .flatMap { context ->
                Mono.justOrEmpty(adjustBasePath(context.apiPath, apiContext.openApi))
                    .flatMap { v ->
                        Mono.justOrEmpty(findMatchingPathValue(v, apiContext.openApi.paths))
                            .switchIfEmpty(Mono.error { ApiException(context, makeErrorResponse(HttpURLConnection.HTTP_NOT_FOUND)) })
                            .flatMap { path ->
                                Mono.justOrEmpty(getOperation(path, apiContext.method)).zipWith(Mono.just(path))
                            }
                            .switchIfEmpty(Mono.error { ApiException(context, makeErrorResponse(HttpURLConnection.HTTP_BAD_METHOD)) })
                            .flatMap(TupleUtils.function { operation, path ->
                                apiResultProvider.provideResult(path, operation, context)
                            })
                            .flatMap { response -> emitResponse(context, response) }
                    }
            }.onErrorResume(ApiException::class.java) { e -> emitResponse(e.requestContext, e.responseContext) }
            .doOnError { e -> logger.error(e.message) }

    private fun makeRequestContext(apiContext: ApiContext, path: String, request: HttpRequest, response: HttpResponse) = apiContext.apiDefinitions.let { apiDefinitions ->
        val apiOptions = ModelPropertyUtils.mergeProperty(path, apiDefinitions, ApiCommonConfigurations<*>::options)
        request.bodyToInputStream().map { inputStream ->
            ApiContextAwareRequestContext(
                apiContext = apiContext, apiDefinitions = apiDefinitions, apiPath = path,
                apiOptions = apiOptions,
                content = readContent(request, inputStream),
                contentType = Optional.ofNullable(request.contentType),
                headers = TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER).apply {
                    ModelPropertyUtils.mergeProperty(path, apiDefinitions, ApiCommonConfigurations<*>::headers)?.request?.let { putAll(it) }
                    putAll(readHeaders(request))
                },
                cookies = request.cookies.associate { c -> c.name to HttpCookie(c.name, c.value) },
                method = request.method, queryParameters = request.queryParameters,
                rawRequest = request, rawResponse = response
            )
        }
    }

    private fun emitResponse(requestContext: ApiContextAwareRequestContext, responseContext: ResponseContext) =
        apiFailureService.checkFailure(requestContext, responseContext.headers)
            .switchIfEmpty(Mono.defer { customizeResponse(responseContext, requestContext) })

    private fun customizeResponse(responseContext: ResponseContext, requestContext: ApiContextAwareRequestContext) =
        responseContext.let { context ->
            ModelPropertyUtils.mergeProperty(requestContext.apiPath, requestContext.apiDefinitions, ApiCommonConfigurations<*>::headers)?.response?.let { responseHeaders ->
                val newHeaders = TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER).apply {
                    putAll(responseHeaders)
                    putAll(context.headers)
                }
                context.mutate().headers(newHeaders).build()
            } ?: context
        }.customize(requestContext)

    private fun ResponseContext.customize(requestContext: ApiContextAwareRequestContext) =
        pluginService.applyPlugin(requestContext, this).map { responseContext ->
            requestContext.apiOptions?.latency?.let {
                HighLatencyResponseContext(responseContext, it.toDuration())
            } ?: responseContext
        }
}

private fun makeErrorResponse(status: Int): ResponseContext = DefaultResponseContext(status = status)


private fun getOperation(item: PathItem, method: String): Optional<Operation> = when (method) {
    "GET" -> Optional.ofNullable(item.get)
    "POST" -> Optional.ofNullable(item.post)
    "HEAD" -> Optional.ofNullable(item.head)
    "PUT" -> Optional.ofNullable(item.put)
    "PATCH" -> Optional.ofNullable(item.patch)
    "DELETE" -> Optional.ofNullable(item.delete)
    "OPTIONS" -> Optional.ofNullable(item.options)
    "TRACE" -> Optional.ofNullable(item.trace)
    else -> Optional.empty()
}

internal fun adjustBasePath(path: String, api: OpenAPI): Optional<String> {
    // servers may contain multiple URLs, so check all
    val maybePath = api.servers.map { server -> URI.create(server.url).path }
            .sortedDescending()
            .map { basePath ->
                when {
                    basePath.isNullOrEmpty() -> path
                    // resolve will add servers of '/' path
                    basePath.isNotEmpty() && basePath != "/" -> {
                        // now the base path must always be subtracted
                        val index: Int = path.indexOf(basePath)
                        if (index < 0) {
                            null
                        } else path.substring(basePath.length)
                    }
                    // '/' case, it must be there then
                    else -> path
                }
            }.firstOrNull { v -> v != null }
    return Optional.ofNullable(maybePath)
}

private fun readHeaders(request: HttpRequest): Map<String, List<String>> = request.headerNames.asSequence().map { n ->
    n to request.getHeaders(n).toList()
}.toMap().toSortedMap(String.CASE_INSENSITIVE_ORDER)

private fun readContent(request: HttpRequest, inputStream: InputStream): Optional<ByteArray> = when (request.method) {
    "GET", "DELETE", "HEAD", "OPTIONS" -> Optional.empty()
    else -> try {
        val size = request.getHeader("Content-Length")?.let(Integer::parseInt) ?: -1
        if (size > 0) {
            Optional.of(inputStream.readNBytes(size))
        } else {
            Optional.ofNullable(inputStream.readAllBytes())
                .filter { b -> b.isNotEmpty() }
        }
    } catch (e: IOException) {
        Optional.empty()
    }
}
data class ApiContextAwareRequestContext(val apiContext: ApiContext,
                                         val apiDefinitions: ApiDefinitions,
                                         val apiOptions: ApiOptions?,
                                         override val apiPath: String,
                                         override val method: String,
                                         override val content: Optional<ByteArray>,
                                         override val contentType: Optional<String>,
                                         override val headers: Map<String, List<String>>,
                                         override val cookies: Map<String, HttpCookie>,
                                         override val queryParameters: Map<String, List<String?>>,
                                         override val rawRequest: HttpRequest,
                                         override val rawResponse: HttpResponse
): RequestContext {
    override val applicationName
        get() = apiContext.context

    val skipValidation
        get() = apiOptions?.shouldValidate == false
}

internal data class DefaultResponseContext(override val status: Int,
                                           override val content: Optional<ByteArray> = Optional.empty(),
                                           override val contentType: Optional<String> = Optional.empty(),
                                           override val headers: Map<String, List<String>> = mapOf()): ResponseContext {
    override fun emitResponse(response: HttpResponse): Publisher<ByteArray> {
        response.status = this.status
        headers.forEach { (k, vs) -> vs.forEach { v -> response.addHeader(k, v) } }
        contentType.ifPresent { t -> response.contentType = t }
        return content.map { v ->
            Mono.just(v)
        }.orElse(Mono.empty())
    }

    override fun mutate() = DefaultResponseContextBuilder(status, content, contentType, headers)
    override fun toString(): String {
        return "DefaultResponseContext(status=$status, content=${content.map { "{size=${it.size}}" }}, contentType=$contentType, headers=$headers)"
    }

    internal data class DefaultResponseContextBuilder(private val status: Int,
                                                      private val content: Optional<ByteArray>,
                                                      private val contentType: Optional<String>,
                                                      private val headers: Map<String, List<String>>): ResponseContext.ResponseContextBuilder {
        override fun status(status: Int) = DefaultResponseContextBuilder(status, content, contentType, headers)
        override fun content(content: ByteArray?) = DefaultResponseContextBuilder(status, Optional.ofNullable(content), contentType, headers)
        override fun contentType(contentType: String?) = DefaultResponseContextBuilder(status, content, Optional.ofNullable(contentType), headers)
        override fun headers(headers: Map<String, List<String>>) = DefaultResponseContextBuilder(status, content, contentType, headers)

        override fun build() = DefaultResponseContext(status, content, contentType, headers)
    }

}

internal data class HighLatencyResponseContext(override val status: Int,
                                               override val content: Optional<ByteArray> = Optional.empty(),
                                               override val contentType: Optional<String> = Optional.empty(),
                                               override val headers: Map<String, List<String>> = mapOf(),
                                               private val interval: Duration): ResponseContext {
    constructor(responseContext: ResponseContext, interval: Duration):
        this(responseContext.status, responseContext.content, responseContext.contentType, responseContext.headers, interval)

    override fun mutate(): ResponseContext.ResponseContextBuilder = throw UnsupportedOperationException("Mutation of this class is not allowed")

    override fun emitResponse(response: HttpResponse): Publisher<ByteArray> {
        response.status = this.status
        contentType.ifPresent { t -> response.contentType = t }
        headers.forEach { (k, vs) -> vs.forEach { v -> response.addHeader(k, v) } }

        // now let's
        return content.map { ba ->
            Flux.concat(ba.map { Flux.just(byteArrayOf(it)).delayElements(interval) })
        }.orElse(Flux.empty())
    }

}
