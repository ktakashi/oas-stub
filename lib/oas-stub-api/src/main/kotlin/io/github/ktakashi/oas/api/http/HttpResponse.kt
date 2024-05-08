package io.github.ktakashi.oas.api.http

/**
 * Wrapper of native Http response
 */
interface HttpResponse {
    var status: Int
    var contentType: String
    fun addHeader(name: String, value: String)
}