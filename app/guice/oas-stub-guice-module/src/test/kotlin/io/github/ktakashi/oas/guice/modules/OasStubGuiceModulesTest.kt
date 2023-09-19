package io.github.ktakashi.oas.guice.modules

import com.google.inject.Guice
import com.google.inject.Injector
import io.github.ktakashi.oas.engine.apis.ApiExecutionService
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.engine.storages.StorageService
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceServerConfiguration
import io.github.ktakashi.oas.guice.injector.createInjector
import io.github.ktakashi.oas.guice.injector.createServerInjector
import io.github.ktakashi.oas.guice.injector.createWebInjector
import io.github.ktakashi.oas.guice.server.OasStubServer
import io.github.ktakashi.oas.plugin.apis.Storage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class OasStubGuiceModulesTest {
    @Test
    fun testModules() {
        val configuration = OasStubGuiceServerConfiguration.builder().build()
        val injector = Guice.createInjector(OasStubGuiceEngineModule(configuration))

        checkInjector(injector)

        val oasInjector = createInjector(configuration)
        checkInjector(oasInjector)

        val oasWebInjector = createWebInjector(configuration)
        checkInjector(oasWebInjector)

        val oasServerInjector = createServerInjector(configuration)
        checkInjector(oasServerInjector)
        val server = oasServerInjector.getInstance(OasStubServer::class.java)
        assertNotNull(server)
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