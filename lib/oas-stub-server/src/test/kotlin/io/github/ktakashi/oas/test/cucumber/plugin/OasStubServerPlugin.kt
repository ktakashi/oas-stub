package io.github.ktakashi.oas.test.cucumber.plugin

import io.cucumber.plugin.EventListener
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestRunStarted
import io.github.ktakashi.oas.server.OasStubServer
import io.github.ktakashi.oas.server.options.OasStubOptions
import io.github.ktakashi.oas.test.cucumber.CustomRoutes

class OasStubServerPlugin: EventListener {
    companion object {
        lateinit var server: OasStubServer
        private lateinit var options: OasStubOptions

        fun setup() {
            options = OasStubOptions.builder()
                .stubOptions()
                .enableRecords(true)
                .addStaticConfiguration("classpath:/static-config.yaml")
                .addRoutesBuilder(CustomRoutes())
                .parent()
                .serverOptions()
                .port(0)
                .httpsPort(0)
                .parent()
                .build()
            server = OasStubServer(options)
            server.start()
        }

        fun cleanup() {
            try {
                server.stop()
            } catch (e: Exception) {
                // do nothing
            }
        }
    }

    override fun setEventPublisher(publisher: EventPublisher) {
        publisher.registerHandlerFor(TestRunStarted::class.java) { _ -> setup() }
        publisher.registerHandlerFor(TestRunFinished::class.java) { _ -> cleanup() }
    }
}