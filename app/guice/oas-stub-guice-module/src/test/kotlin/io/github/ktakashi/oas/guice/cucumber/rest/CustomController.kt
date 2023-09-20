package io.github.ktakashi.oas.guice.cucumber.rest

import io.github.ktakashi.oas.web.annotations.Delayable
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@Path("/custom")
class CustomController {
    @Delayable(context = "custom", path = "/ok1")
    @GET
    @Path("/ok1")
    fun getMono1() = get1()

    @Delayable(context = "custom", path = "/ok2")
    @GET
    @Path("/ok2")
    fun getFlux1(): Response = Response.ok(listOf("OK", "OK", "OK")).build()

    @Delayable(context = "custom", path = "/ok3")
    @GET
    @Path("/ok3")
    fun getCompletionStage1(): CompletionStage<Response> = CompletableFuture.completedStage(Response.ok().entity("OK").build())


    @Delayable(context = "custom", path = "/ok4")
    @GET
    @Path("/ok4")
    fun get1(): Response = Response.ok().entity("OK").build()
}
