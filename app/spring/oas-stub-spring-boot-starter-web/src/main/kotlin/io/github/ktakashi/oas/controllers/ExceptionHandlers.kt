package io.github.ktakashi.oas.controllers

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("io.github.ktakashi.oas.controllers.ExceptionMapper")

class GenericExceptionMapper: ExceptionMapper<Exception> {
    override fun toResponse(exception: Exception): Response {
        logger.error("Unhandled exception: {}", exception.message, exception)
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(exception.message)
            .build()
    }

}
