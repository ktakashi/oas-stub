package io.github.ktakashi.oas.engine.apis.record

import io.github.ktakashi.oas.api.http.RequestContext
import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage

class ApiRecorder(private val persistentStorage: PersistentStorage,
                  private val sessionStorage: SessionStorage) {
    fun addApiRecord(name: String, request: RequestContext, response: ResponseContext) = sessionStorage.addApiRecord(name, request, response)

    fun getApiRecords(name: String) = sessionStorage.getApiRecords(name)

    fun clearApiRecords(name: String) = sessionStorage.clearApiRecords(name)

    fun clearAllApiRecords() = persistentStorage.getNames().forEach(this::clearApiRecords)
}