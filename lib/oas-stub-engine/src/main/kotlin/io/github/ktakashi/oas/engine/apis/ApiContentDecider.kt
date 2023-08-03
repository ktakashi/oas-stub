package io.github.ktakashi.oas.engine.apis

import io.github.ktakashi.oas.plugin.apis.ResponseContext
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

sealed interface ContentDecision
data class ContentFound(val status: Int, val content: Content): ContentDecision
data class ContentNotFound(val responseContext: ResponseContext): ContentDecision
@Named @Singleton
class ApiContentDecider
@Inject constructor(private val validators: Set<ApiRequestValidator>){
    fun decideContent(requestContext: ApiContextAwareRequestContext, operation: Operation): ContentDecision {
        TODO("implement")
    }
}
