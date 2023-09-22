package io.github.ktakashi.oas.test.cucumber.plugins

import io.cucumber.plugin.EventListener
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestRunStarted

// Hazelcast and MongoDB :)
class HazelDbPlugin: EventListener {
    override fun setEventPublisher(publisher: EventPublisher) {
        publisher.registerHandlerFor(TestRunStarted::class.java) { _ -> setup() }
        publisher.registerHandlerFor(TestRunFinished::class.java) { _ -> cleanup() }
    }

    private fun setup() {
        HazelcastPlugin.setup()
        MongodbPlugin.setup()
        System.setProperty("spring.profiles.active", "hazeldb")
    }

    private fun cleanup() {
        HazelcastPlugin.cleanup()
        MongodbPlugin.cleanup()
    }
}
