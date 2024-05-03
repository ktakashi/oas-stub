package io.github.ktakashi.oas.server.io

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufInputStream
import java.io.InputStream
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServerRequest

internal fun HttpServerRequest.bodyToInputStream(): Mono<InputStream> = this.receive()
    .aggregate()
    .asInputStream()
    .switchIfEmpty(Mono.just(InputStream.nullInputStream()))

internal inline fun <reified T> HttpServerRequest.bodyToMono(objectMapper: ObjectMapper): Mono<T>
        = this.bodyToInputStream().map { objectMapper.readValue(it, T::class.java) }