package io.github.ktakashi.oas.engine.apis.json

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.apis.ApiDataValidator
import io.github.ktakashi.oas.engine.apis.ApiValidationResult
import io.github.ktakashi.oas.engine.apis.success
import io.github.ktakashi.oas.engine.validators.Validator
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.Schema
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

@Named @Singleton
class JsonOpenApi31DataValidator
@Inject constructor(private val objectMapper: ObjectMapper,
                    private val validators: Set<Validator<Any>>) : ApiDataValidator(SpecVersion.V30), JsonMediaSupport {
    // TODO implement it,
    // NOTE since OAS 3.1.x, it only returns JsonSchema, so we need to implement JSON Schema :(
    //      we might be able to use one of the implementation listed here
    //      - https://json-schema.org/implementations.html#validator-java
    override fun validate(input: ByteArray, schema: Schema<*>): ApiValidationResult = success // dummy

}
