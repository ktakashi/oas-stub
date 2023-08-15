package io.github.ktakashi.oas.engine.apis

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.parsers.ParsingService
import io.github.ktakashi.oas.engine.plugins.PluginService
import io.github.ktakashi.oas.engine.storages.StorageService
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.ApiOptions
import io.github.ktakashi.oas.model.MergeableApiConfig
import io.github.ktakashi.oas.plugin.apis.RequestContext
import io.github.ktakashi.oas.plugin.apis.ResponseContext
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.IOException
import java.net.HttpCookie
import java.net.URI
import java.util.Optional
import org.apache.http.HttpStatus
import org.glassfish.jersey.uri.UriTemplate

data class ApiContext(val context: String, val method: String, val openApi: OpenAPI, val apiDefinitions: ApiDefinitions)

interface ApiContextService {
    /**
     * Retrieves [ApiContext] from the [request]
     *
     * If context is not found, then the result would be empty
     */
    fun getApiContext(request: HttpServletRequest): Optional<ApiContext>
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
    fun executeApi(apiContext: ApiContext, request: HttpServletRequest, response: HttpServletResponse): ResponseContext
}

/**
 * The API registration interface
 *
 * This interface should be used to implement admin APIs, e.g. REST controllers.
 */
interface ApiRegistrationService: ApiContextService {
    /**
     * Retrieves the [ApiDefinitions] associated to the [name]
     */
    fun getApiDefinitions(name: String): Optional<ApiDefinitions>

    /**
     * Saves the [apiDefinitions] with association to the [name]
     */
    fun saveApiDefinitions(name: String, apiDefinitions: ApiDefinitions): Boolean

    /**
     * Deletes API definition from the [name]
     */
    fun deleteApiDefinitions(name: String): Boolean
}

@Named @Singleton
class DefaultApiService
@Inject constructor(private val storageService: StorageService,
                    private val parsingService: ParsingService,
                    private val apiPathService: ApiPathService,
                    private val apiRequestPathVariableValidator: ApiRequestPathVariableValidator,
                    private val apiResultProvider: ApiResultProvider,
                    private val pluginService: PluginService): ApiExecutionService, ApiRegistrationService {
    override fun getApiContext(request: HttpServletRequest): Optional<ApiContext> =
            apiPathService.extractApplicationName(request.requestURI).flatMap { name ->
                storageService.getApiDefinitions(name).flatMap { apiDefinitions ->
                    storageService.getOpenApi(name)
                            .map { def -> ApiContext(openApi = def, context = name, method = request.method, apiDefinitions = apiDefinitions) }
                }
            }

    override fun getApiDefinitions(name: String): Optional<ApiDefinitions> = storageService.getApiDefinitions(name)
    override fun saveApiDefinitions(name: String, apiDefinitions: ApiDefinitions): Boolean = parsingService.parse(apiDefinitions.specification)
            .filter { openApi -> apiDefinitions.configurations.keys.all { path ->
                adjustBasePath(path, openApi).map { p -> apiPathService.findMatchingPath(p, openApi.paths).isPresent }
                        .orElse(false)
            }}
            .map { openApi -> apiDefinitions.updateSpecification(parsingService.toYaml(openApi)) }
            .map { def -> storageService.saveApiDefinitions(name, def) }
            .orElse(false)

    override fun deleteApiDefinitions(name: String): Boolean = storageService.deleteApiDefinitions(name)

    override fun executeApi(apiContext: ApiContext, request: HttpServletRequest, response: HttpServletResponse): ResponseContext {
        val appName = apiContext.context
        val path = apiPathService.extractApiPath(appName, request.requestURI)
        val requestContext = makeRequestContext(apiContext, path, request, response)
        val adjustedPath = adjustBasePath(path, apiContext.openApi)
        val pathItem = adjustedPath.flatMap { v -> apiPathService.findMatchingPathValue(v, apiContext.openApi.paths) }
        if (pathItem.isEmpty) {
            return emitResponse(requestContext, makeErrorResponse(HttpStatus.SC_NOT_FOUND))
        }
        val operation = getOperation(pathItem.get(), apiContext.method)
        if (operation.isEmpty) {
            return emitResponse(requestContext, makeErrorResponse(HttpStatus.SC_METHOD_NOT_ALLOWED))
        }
        // TODO failure

        val responseContext = if (requestContext.skipValidation) {
            apiResultProvider.provideResult(operation.get(), requestContext)
        } else {
            adjustedPath.flatMap { v ->
                apiPathService.findMatchingPath(v, apiContext.openApi.paths)
                        .flatMap { p -> apiRequestPathVariableValidator.validate(v, p, operation.get()) }
                        .map<ResponseContext> { p ->
                            DefaultResponseContext(status = p.first, content = p.second, contentType = Optional.of(APPLICATION_PROBLEM_JSON))
                        }
            }.orElseGet { apiResultProvider.provideResult(operation.get(), requestContext) }
        }
        return emitResponse(requestContext, responseContext)
    }

    private fun makeRequestContext(apiContext: ApiContext, path: String, request: HttpServletRequest, response: HttpServletResponse) =
            apiContext.apiDefinitions.let { apiDefinitions ->
                ApiContextAwareRequestContext(
                        apiContext = apiContext, apiDefinitions = apiDefinitions, apiPath = path,
                        apiOptions = mergeProperty(path, apiDefinitions, ApiCommonConfigurations::options),
                        content = readContent(request),
                        contentType = Optional.ofNullable(request.contentType),
                        headers = mergeProperty(path, apiDefinitions, ApiCommonConfigurations::headers).request + readHeaders(request),
                        cookies = request.cookies?.associate { c -> c.name to HttpCookie(c.name, c.value) } ?: mapOf(),
                        method = request.method, queryParameters = parseQueryParameters(request.queryString),
                        rawRequest = request, rawResponse = response)
            }

    private fun emitResponse(requestContext: ApiContextAwareRequestContext, responseContext: ResponseContext): ResponseContext =
        responseContext.let { context ->
            val responseHeaders = requestContext.apiDefinitions.headers.response
            if (responseHeaders.isEmpty()) {
                context
            } else {
                context.from().headers(responseHeaders + context.headers).build()
            }
        }.customize(requestContext)

    private fun ResponseContext.customize(requestContext: RequestContext) = pluginService.applyPlugin(requestContext, this)

    private fun <T: MergeableApiConfig<T>> mergeProperty(path: String, d: ApiDefinitions, propertyRetriever: (ApiCommonConfigurations) -> T) = apiPathService.findMatchingPathValue(path, d.configurations)
            .map(propertyRetriever)
            .map { o -> o.merge(propertyRetriever(d)) }
            .orElseGet { propertyRetriever(d) }
}

@Named @Singleton
class ApiRequestPathVariableValidator
@Inject constructor(private val requestParameterValidator: ApiRequestParameterValidator,
                    private val objectMapper: ObjectMapper) {
    fun validate(path: String, template: String, operation: Operation): Optional<Pair<Int, Optional<ByteArray>>> {
        val uriTemplate = UriTemplate(template)
        val matcher = uriTemplate.pattern.match(path)
        val result = uriTemplate.templateVariables.flatMapIndexed { i, name ->
            operation.parameters.map { parameter ->
                when (parameter.`in`) {
                    "path" ->
                        if (parameter.name == name) {
                            val value = matcher.group(i + 1)
                            requestParameterValidator.validateParameterList(parameter, listOf(value))
                        } else success
                    else -> success
                }
            }
        }.fold(success) { a, b -> a.merge(b) }
        if (result.isValid) {
            return Optional.empty()
        }
        val body = result.toJsonProblemDetails(HttpStatus.SC_BAD_REQUEST, objectMapper)
        return Optional.of(HttpStatus.SC_BAD_REQUEST to body)
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

private val QUERY_PARAM_PATTERN = Regex("([^&=]+)(=?)([^&]+)?");
private fun parseQueryParameters(s: String?): Map<String, List<String?>> = s?.let {
    QUERY_PARAM_PATTERN.findAll(it).map { m ->
        val n = m.groupValues[1]
        val eq = m.groups[2]?.value
        n to (m.groups[3]?.value ?: if (!eq.isNullOrEmpty()) "" else null)
    }
}?.groupBy({ p -> p.first }, { p -> p.second }) ?: mapOf()
private fun readHeaders(request: HttpServletRequest): Map<String, List<String>> = request.headerNames.asSequence().map { n ->
    n to request.getHeaders(n).toList()
}.toMap().toSortedMap(String.CASE_INSENSITIVE_ORDER)

private fun readContent(request: HttpServletRequest): Optional<ByteArray> = when (request.method) {
    "GET", "DELETE", "HEAD", "OPTIONS" -> Optional.empty()
    else -> try {
        val size = request.getHeader("Content-Length")?.let(Integer::parseInt) ?: -1
        val inputStream = request.inputStream
        if (size > 0) {
            Optional.of(inputStream.readNBytes(size))
        } else {
            val b = inputStream.readAllBytes()
            if (b.isEmpty()) {
                Optional.empty()
            } else {
                Optional.of(b)
            }
        }
    } catch (e: IOException) {
        Optional.empty()
    }
}
data class ApiContextAwareRequestContext(val apiContext: ApiContext,
                                         val apiDefinitions: ApiDefinitions,
                                         val apiOptions: ApiOptions,
                                         override val apiPath: String,
                                         override val method: String,
                                         override val content: Optional<ByteArray>,
                                         override val contentType: Optional<String>,
                                         override val headers: Map<String, List<String>>,
                                         override val cookies: Map<String, HttpCookie>,
                                         override val queryParameters: Map<String, List<String?>>,
                                         override val rawRequest: HttpServletRequest,
                                         override val rawResponse: HttpServletResponse): RequestContext {
    override val applicationName
        get() = apiContext.context

    val skipValidation
        get() = apiOptions.shouldValidate == false
}

internal data class DefaultResponseContext(override val status: Int,
                                           override val content: Optional<ByteArray> = Optional.empty(),
                                           override val contentType: Optional<String> = Optional.empty(),
                                           override val headers: Map<String, List<String>> = mapOf()): ResponseContext {
    override fun emitResponse(response: HttpServletResponse) {
        content.ifPresent { v -> response.outputStream.write(v) }
        contentType.ifPresent { t -> response.contentType = t }
        headers.forEach { (k, vs) -> vs.forEach { v -> response.addHeader(k, v) } }
        response.status = this.status
    }

    override fun from() = DefaultResponseContextBuilder(status, content, contentType, headers)

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
