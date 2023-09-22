package io.github.ktakashi.oas.guice.storages.apis

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Module

fun interface OasStubSessionStorageModuleCreator {
    fun createSessionStorage(objectMapper: ObjectMapper): Module
}
fun interface OasStubPersistentStorageModuleCreator {
    fun createPersistentStorage(objectMapper: ObjectMapper): Module
}