@file:JvmName("OasStubStorageApiPropertyNames")
package io.github.ktakashi.oas.storages.apis.conditions

/**
 * Base property name of OAS stub storage properties.
 *
 * This *should not* be used custom autoconfiguration.
 */
const val OAS_STUB_STORAGE = "oas.stub.storage"
const val OAS_STUB_STORAGE_TYPE = "${OAS_STUB_STORAGE}.type"

/**
 * Spring Boot property name for session storage type
 */
const val OAS_STUB_STORAGE_TYPE_SESSION = "${OAS_STUB_STORAGE_TYPE}.session"

/**
 * Spring Boot property name for persistent storage type
 */
const val OAS_STUB_STORAGE_TYPE_PERSISTENT = "${OAS_STUB_STORAGE_TYPE}.persistent"

/**
 * In-memory session storage autoconfiguration module name
 *
 * Make this module to be loaded *after* your autoconfigure module
 * ```kotlin
 * @AutoConfiguration(
 *         beforeName = [OAS_STUB_INMEMORY_SESSION_STORAGE_MODULE],
 * )
 * ```
 */
const val OAS_STUB_INMEMORY_SESSION_STORAGE_MODULE = "io.github.ktakashi.oas.storages.inmemory.configurations.AutoInMemorySessionStorageConfiguration"
/**
 * In-memory persistent storage autoconfiguration module name
 *
 * Make this module to be loaded *after* your autoconfigure module
 * ```kotlin
 * @AutoConfiguration(
 *         beforeName = [OAS_STUB_INMEMORY_PERSISTENT_STORAGE_MODULE],
 * )
 * ```
 */
const val OAS_STUB_INMEMORY_PERSISTENT_STORAGE_MODULE = "io.github.ktakashi.oas.storages.inmemory.configurations.AutoInMemoryPersistentStorageConfiguration"