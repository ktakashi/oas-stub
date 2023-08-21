package io.github.ktakashi.oas.cucumber.plugins

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import io.cucumber.plugin.EventListener
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestRunStarted

class HazelcastPlugin: EventListener {
    companion object {
        lateinit var hazelcastInstance: HazelcastInstance
    }
    override fun setEventPublisher(publisher: EventPublisher) {
        publisher.registerHandlerFor(TestRunStarted::class.java) { _ -> setup() }
        publisher.registerHandlerFor(TestRunFinished::class.java) { _ -> cleanup() }
    }

    private fun setup() {
        hazelcastInstance = Hazelcast.newHazelcastInstance()
        System.setProperty("spring.profiles.active", "hazelcast")
    }

    private fun cleanup() {
        hazelcastInstance.shutdown()
        System.clearProperty("spring.profiles.active")
    }
}
