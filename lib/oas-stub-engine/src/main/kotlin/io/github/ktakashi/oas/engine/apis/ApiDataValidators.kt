package io.github.ktakashi.oas.engine.apis

import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.Schema

abstract class ApiDataValidator(version: SpecVersion): MediaSupport, OpenApiVersionSupport(version) {
    abstract fun validate(input: ByteArray, schema: Schema<*>): ApiValidationResult
}
