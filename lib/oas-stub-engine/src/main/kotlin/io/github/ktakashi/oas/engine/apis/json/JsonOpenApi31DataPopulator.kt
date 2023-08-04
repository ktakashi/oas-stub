package io.github.ktakashi.oas.engine.apis.json

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.apis.AbstractApiDataPopulator
import io.github.ktakashi.oas.engine.apis.ApiAnyDataPopulator
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.Schema
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

@Named @Singleton
class JsonOpenApi31DataPopulator
@Inject constructor(private val objectMapper: ObjectMapper): JsonMediaSupport, ApiAnyDataPopulator, AbstractApiDataPopulator(SpecVersion.V30) {
    // TODO implement it
    override fun populate(schema: Schema<*>): ByteArray = byteArrayOf()

}
