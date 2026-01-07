package io.github.ktakashi.oas.test.cucumber.plugins

import io.cucumber.plugin.EventListener
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestRunStarted
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class HazelcastContainer(dockerImageName: DockerImageName): GenericContainer<HazelcastContainer>(dockerImageName) {
    constructor(): this("hazelcast/hazelcast:5.6.0-slim-jdk21")
    constructor(imageName: String): this(DockerImageName.parse(imageName))
}

class HazelcastPlugin: EventListener {
    companion object {
        val HAZELCAST_CONTAINER: HazelcastContainer = HazelcastContainer()
            .withExposedPorts(5701)
        fun setup() {
            HAZELCAST_CONTAINER.start()
            System.setProperty("hazelcast.cluster.members", "${HAZELCAST_CONTAINER.host}:${HAZELCAST_CONTAINER.getMappedPort(5701)}")
            System.setProperty("spring.profiles.active", "hazelcast")
        }

        fun cleanup() {
            HAZELCAST_CONTAINER.stop()
            System.clearProperty("spring.profiles.active")
        }
    }
    override fun setEventPublisher(publisher: EventPublisher) {
        publisher.registerHandlerFor(TestRunStarted::class.java) { _ -> setup() }
        publisher.registerHandlerFor(TestRunFinished::class.java) { _ -> cleanup() }
    }
}
