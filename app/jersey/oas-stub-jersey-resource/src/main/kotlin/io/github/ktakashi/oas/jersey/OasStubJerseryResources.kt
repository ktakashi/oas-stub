package io.github.ktakashi.oas.jersey

import io.github.ktakashi.oas.web.rests.ApiController
import io.github.ktakashi.oas.web.rests.ContextConfigurationsController
import io.github.ktakashi.oas.web.rests.ContextController
import io.github.ktakashi.oas.web.rests.ContextDataController
import io.github.ktakashi.oas.web.rests.ContextDelayController
import io.github.ktakashi.oas.web.rests.ContextHeadersController
import io.github.ktakashi.oas.web.rests.ContextOptionsController
import io.github.ktakashi.oas.web.rests.DataConfigurationsController
import io.github.ktakashi.oas.web.rests.DelayConfigurationsController
import io.github.ktakashi.oas.web.rests.HeadersConfigurationsController
import io.github.ktakashi.oas.web.rests.MetricsController
import io.github.ktakashi.oas.web.rests.OptionsConfigurationsController
import io.github.ktakashi.oas.web.rests.PluginConfigurationsController
import io.swagger.v3.jaxrs2.Reader
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration
import jakarta.ws.rs.core.Application
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.server.ServerProperties
import org.slf4j.LoggerFactory

open class OasStubResourceConfig(adminPrefix: String): ResourceConfig() {
    init {
        this.register(ApiController::class.java)
        this.register(ContextController::class.java)
        this.register(ContextOptionsController::class.java)
        this.register(ContextConfigurationsController::class.java)
        this.register(ContextHeadersController::class.java)
        this.register(ContextDataController::class.java)
        this.register(ContextDelayController::class.java)

        this.register(PluginConfigurationsController::class.java)
        this.register(HeadersConfigurationsController::class.java)
        this.register(OptionsConfigurationsController::class.java)
        this.register(DataConfigurationsController::class.java)
        this.register(DelayConfigurationsController::class.java)

        this.register(MetricsController::class.java)

        this.register(GenericExceptionMapper::class.java)

        this.property(ServerProperties.LOCATION_HEADER_RELATIVE_URI_RESOLUTION_DISABLED, true)
        this.property(OAS_APPLICATION_PATH_CONFIG, adminPrefix)
        this.register(OpenApiResource::class.java)
    }
}

private val logger = LoggerFactory.getLogger("io.github.ktakashi.oas.jersey.ExceptionMapper")

class GenericExceptionMapper: ExceptionMapper<Exception> {
    override fun toResponse(exception: Exception): Response {
        logger.error("Unhandled exception: {}", exception.message, exception)
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(exception.message)
            .build()
    }
}

const val OAS_APPLICATION_PATH_CONFIG = "OasStubApplicationPath"

class ApplicationConfigAwareReader: Reader {
    constructor(openAPIConfiguration: OpenAPIConfiguration): super(openAPIConfiguration)
    constructor(): super()

    private var application: Application? = null
    override fun resolveApplicationPath(): String {
        if (application != null && application is ResourceConfig) {
            val value = (application as ResourceConfig).getProperty(OAS_APPLICATION_PATH_CONFIG)
            if (value != null) {
                return value as String
            }
        }
        return super.resolveApplicationPath()
    }

    override fun setApplication(application: Application?) {
        this.application = application
        super.setApplication(application)
    }
}