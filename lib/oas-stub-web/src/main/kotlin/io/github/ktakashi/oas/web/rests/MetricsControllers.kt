package io.github.ktakashi.oas.web.rests

import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.web.annotations.Admin
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Admin
@Path("/metrics")
@Tag(name = "API info", description = "API info")
@Named @Singleton
class MetricsController
@Inject constructor(private val apiObserver: ApiObserver) {
    @GET
    @Path("/{context}")
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("context") context: String): Response = apiObserver.getApiMetrics(context)
        .map { v -> Response.ok(v).build() }
        .orElseGet { Response.status(Response.Status.NOT_FOUND).build() }

    @DELETE
    fun delete() = apiObserver.clearApiMetrics()
}