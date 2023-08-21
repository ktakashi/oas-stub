package io.github.ktakashi.oas.cucumber.plugins

import io.cucumber.plugin.EventListener
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestRunStarted

// Hazelcast and MongoDB :)
class HazelDbPlugin: EventListener {
    private val hazelcastPlugin = HazelcastPlugin()
    private val mongodbPlugin = MongodbPlugin()
    override fun setEventPublisher(publisher: EventPublisher) {
        publisher.registerHandlerFor(TestRunStarted::class.java) { _ -> setup() }
        publisher.registerHandlerFor(TestRunFinished::class.java) { _ -> cleanup() }
    }

    private fun setup() {
        hazelcastPlugin.setup()
        mongodbPlugin.setup()
        System.setProperty("spring.profiles.active", "hazeldb")
    }

    private fun cleanup() {
        hazelcastPlugin.cleanup()
        mongodbPlugin.cleanup()
    }
}
