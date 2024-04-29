package io.github.ktakashi.oas.test.cucumber

import io.cucumber.java.After
import io.cucumber.java.Before
import io.github.ktakashi.oas.guice.server.OasStubServer
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class ServerStep
@Inject constructor(private val oasStubServer: OasStubServer) {

    @Before
    fun startServer() {
        oasStubServer.start()
    }

    @After
    fun stop() = oasStubServer.stop()
}