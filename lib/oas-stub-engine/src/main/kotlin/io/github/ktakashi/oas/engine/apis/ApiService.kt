package io.github.ktakashi.oas.engine.apis

import io.github.ktakashi.oas.engine.plugins.PluginService
import io.github.ktakashi.oas.engine.storages.StorageService
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
import java.net.URI
import java.util.Optional
import org.apache.http.HttpStatus

data class ApiContext(val context: String, val method: String, val apiDefinition: OpenAPI)

@Named @Singleton
class ApiService
@Inject constructor(private val storageService: StorageService,
                    private val apiPathService: ApiPathService,
                    private val apiResultProvider: ApiResultProvider,
                    private val pluginService: PluginService) {
    fun getApiContext(request: HttpServletRequest): Optional<ApiContext> =
            apiPathService.extractApplicationName(request.requestURI).flatMap { name ->
                storageService.getApiDefinition(name)
                        .map { def -> ApiContext(apiDefinition = def, context = name, method = request.method) }
            }

    fun executeApi(apiContext: ApiContext, request: HttpServletRequest, response: HttpServletResponse): ResponseContext {
        val appName = apiContext.context
        val path = apiPathService.extractApiPath(appName, request.requestURI)
        val requestContext = makeRequestContext(apiContext, path, request, response)
        val pathItem = adjustBasePath(path, apiContext.apiDefinition).flatMap { v -> apiPathService.findMatchingPath(v, apiContext.apiDefinition.paths) }
        if (pathItem.isEmpty) {
            return emitResponse(response, requestContext, makeErrorResponse(HttpStatus.SC_NOT_FOUND))
        }
        val operation = getOperation(pathItem.get(), apiContext.method)
        if (operation.isEmpty) {
            return emitResponse(response, requestContext, makeErrorResponse(HttpStatus.SC_METHOD_NOT_ALLOWED))
        }
        // TODO failure


        val responseContext = apiResultProvider.provideResult(operation.get(), requestContext)
        return emitResponse(response, requestContext, responseContext)
    }

    private fun makeRequestContext(apiContext: ApiContext, path: String, request: HttpServletRequest, response: HttpServletResponse) =
            ApiContextAwareRequestContext(apiContext = apiContext, apiPath = path, content = readContent(request),
                    contentType = Optional.ofNullable(request.contentType), headers = readHeaders(request),
                    method = request.method, queryParameters = parseQueryParameters(request.queryString),
                    attributes = populateAttribute(apiContext, path),
                    rawRequest = request, rawResponse = response)

    private fun emitResponse(response: HttpServletResponse, requestContext: RequestContext, responseContext: ResponseContext): ResponseContext =
        responseContext.customize(requestContext).apply {
            // apply customiser
            content.ifPresent { v -> response.outputStream.write(v) }
            contentType.ifPresent { t -> response.contentType = t }
            headers.forEach { (k, vs) -> vs.forEach { v -> response.addHeader(k, v) } }
            response.status = responseContext.status
        }

    private fun populateAttribute(apiContext: ApiContext, path: String): Map<String, Any> {
        return mapOf()
    }

    private fun ResponseContext.customize(requestContext: RequestContext) = pluginService.applyPlugin(requestContext, this)
}

private fun makeErrorResponse(status: Int) = ResponseContext(status)


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

private fun adjustBasePath(path: String, api: OpenAPI): Optional<String> {
    // Note: V2 (a.k.a Swagger) has basePath and this is merged into servers array in V3
    // So on V2, it has only one server max ;)
    val servers = api.servers
    if (servers.isNotEmpty()) {
        // Okay we get the first one and believe that the base paths are the same
        // Otherwise we need to think nice way to resolve.
        val serverUri = URI.create(servers[0].url)
        val basePath = serverUri.path
        // resolve will add servers of '/' path
        if (basePath.isNotEmpty() && "/" != basePath) {
            // now the base path must always be subtracted
            val index: Int = path.indexOf(basePath)
            return if (index < 0) {
                Optional.empty()
            } else Optional.of(path.substring(basePath.length))
        }
    }
    return Optional.of(path)
}

private val QUERY_PARAM_PATTERN = Regex("([^&=]+)(=?)([^&]+)?");
private fun parseQueryParameters(s: String?): Map<String, List<String?>> = s?.let {
    QUERY_PARAM_PATTERN.findAll(it).map { m ->
        val n = m.groupValues[0]
        val eq = m.groups[1]?.value
        n to (m.groups[2]?.value ?: if (!eq.isNullOrEmpty()) "" else null)
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
                                         override val apiPath: String,
                                         override val method: String,
                                         override val content: Optional<ByteArray>,
                                         override val contentType: Optional<String>,
                                         override val attributes: Map<String, Any>,
                                         override val headers: Map<String, List<String>>,
                                         override val queryParameters: Map<String, List<String?>>,
                                         override val rawRequest: HttpServletRequest,
                                         override val rawResponse: HttpServletResponse): RequestContext {
    override val applicationName
        get() = apiContext.context
}
