package io.github.ktakashi.oas.api.http

import java.io.OutputStream

/**
 * Wrapper of native Http response
 */
interface HttpResponse {
    var status: Int
    var contentType: String
    fun addHeader(name: String, value: String)
    val outputStream: OutputStream
}