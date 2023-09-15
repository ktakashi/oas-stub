package io.github.ktakashi.oas.guice.modules

import com.google.inject.Guice
import com.google.inject.Injector
import io.github.ktakashi.oas.engine.apis.ApiExecutionService
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.engine.storages.StorageService
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceConfiguration
import io.github.ktakashi.oas.guice.injector.createGuiceInjector
import io.github.ktakashi.oas.plugin.apis.Storage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class OasStubGuiceModulesTest {
    @Test
    fun testModules() {
        val injector = Guice.createInjector(
            OasStubInMemoryPersistentStorageModule(),
            OasStubInMemorySessionStorageModule(),
            OasStubGuiceEngineModule("/oas"))

        checkInjector(injector)

        val oasInjector = createGuiceInjector(OasStubGuiceConfiguration.builder().build())
        checkInjector(oasInjector)
    }

    private fun checkInjector(injector: Injector) {
        val storageService = injector.getBinding(StorageService::class.java).provider.get()
        assertNotNull(storageService)

        val executionService = injector.getBinding(ApiExecutionService::class.java).provider.get()
        val registrationService = injector.getBinding(ApiRegistrationService::class.java).provider.get()
        assertNotNull(executionService)
        assertNotNull(registrationService)
        assertEquals(executionService, registrationService)

        val storage = injector.getBinding(Storage::class.java).provider.get()
        val sessionStorage = injector.getBinding(SessionStorage::class.java).provider.get()
        assertNotNull(storage)
        assertNotNull(sessionStorage)
        assertEquals(storage, sessionStorage)
    }
}