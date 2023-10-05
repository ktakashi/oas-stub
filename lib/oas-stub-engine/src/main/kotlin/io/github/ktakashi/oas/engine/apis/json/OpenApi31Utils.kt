package io.github.ktakashi.oas.engine.apis.json

import io.swagger.v3.oas.models.media.Schema

fun guessType(schema: Schema<*>) = schema.type
    ?: schema.types?.first()
    // okay, we need to guess...
    ?: when {
        schema.properties != null -> "object"
        schema.items != null -> "array"
        else -> "any"
    }