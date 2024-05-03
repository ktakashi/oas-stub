package io.github.ktakashi.oas.server.io

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufInputStream
import java.io.InputStream
import java.util.stream.Collectors
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerRequest

internal class ByteBufListInputStream(private val byteBufs: List<ByteBuf>): InputStream() {
    private var index = 0;
    private var pos = 0

    // TODO implement read(byte[], int, int)
    override fun read(): Int = readInternal()

    private fun readInternal(): Int = if (index < byteBufs.size) {
        val buf = byteBufs[index]
        if (pos < buf.readerIndex()) {
            buf.getByte(pos++).toInt()
        } else {
            index++
            pos = 0
            readInternal()
        }
    } else {
        -1
    }
}

internal fun HttpServerRequest.bodyToInputStream(): Mono<InputStream> = this.receiveContent()
    .map { it.content() }
    .collectList()
    .map<InputStream> { ByteBufListInputStream(it) }
    .switchIfEmpty(Mono.just(InputStream.nullInputStream()))

internal inline fun <reified T> HttpServerRequest.bodyToMono(objectMapper: ObjectMapper): Mono<T>
= this.bodyToInputStream().map { objectMapper.readValue(it, T::class.java) }