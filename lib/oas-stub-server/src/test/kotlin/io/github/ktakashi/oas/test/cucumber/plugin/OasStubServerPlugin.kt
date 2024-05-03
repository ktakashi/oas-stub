package io.github.ktakashi.oas.test.cucumber.plugin

import io.cucumber.plugin.EventListener
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestRunStarted
import io.github.ktakashi.oas.server.OasStubServer
import io.github.ktakashi.oas.server.options.OasStubServerOptions

class OasStubServerPlugin: EventListener {
    companion object {
        lateinit var server: OasStubServer
        lateinit var options: OasStubServerOptions
    }
    private fun setup() {
        options = OasStubServerOptions.builder().build()
        server = OasStubServer(options)
        server.start()
    }

    private fun cleanup() {
        server.stop()
    }
    override fun setEventPublisher(publisher: EventPublisher) {
        publisher.registerHandlerFor(TestRunStarted::class.java) { _ -> setup() }
        publisher.registerHandlerFor(TestRunFinished::class.java) { _ -> cleanup() }
    }
}