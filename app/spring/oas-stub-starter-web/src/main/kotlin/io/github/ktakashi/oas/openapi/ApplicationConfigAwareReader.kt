package io.github.ktakashi.oas.openapi

import io.swagger.v3.jaxrs2.Reader
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration
import jakarta.ws.rs.core.Application
import org.glassfish.jersey.server.ResourceConfig

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