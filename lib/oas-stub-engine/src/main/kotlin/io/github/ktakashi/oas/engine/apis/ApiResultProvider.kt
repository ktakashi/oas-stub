package io.github.ktakashi.oas.engine.apis

import io.github.ktakashi.oas.plugin.apis.ResponseContext
import io.swagger.v3.oas.models.Operation
import jakarta.inject.Named
import jakarta.inject.Singleton

@Named @Singleton
class ApiResultProvider {
    fun provideResult(opetaion: Operation, requestContext: ApiContextAwareRequestContext): ResponseContext {
        TODO("implement it")
    }
}
