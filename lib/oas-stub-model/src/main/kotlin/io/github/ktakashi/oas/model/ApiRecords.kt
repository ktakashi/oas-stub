package io.github.ktakashi.oas.model

import java.util.Optional

data class ApiRequestRecord(val contentType: Optional<String>,
                            val headers: Map<String, List<String>>,
                            val cookies: Map<String, String>,
                            val body: Optional<ByteArray>)
data class ApiResponseRecord(val status: Int,
                             val contentType: Optional<String>,
                             val headers: Map<String, List<String>>,
                             val body: Optional<ByteArray>)

data class ApiRecord(val method: String,
                     val path: String,
                     val request: ApiRequestRecord,
                     val response: ApiResponseRecord)

data class ApiRecords(val records: MutableList<ApiRecord> = mutableListOf()) {
    fun addApiRecord(apiRecord: ApiRecord) = apply { records.add(apiRecord) }
}