package io.github.ktakashi.oas.test.cucumber.plugins

import io.cucumber.plugin.EventListener
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestRunStarted
import org.testcontainers.mongodb.MongoDBContainer

class MongodbPlugin: EventListener {
    companion object {
        val MONGO_CONTAINER: MongoDBContainer = MongoDBContainer("mongodb/mongodb-community-server:7.0.26-ubuntu2204")
            .withExposedPorts(27017)

        fun setup() {
            MONGO_CONTAINER.start()
            System.setProperty("mongodb.host", MONGO_CONTAINER.host)
            System.setProperty("mongodb.port", MONGO_CONTAINER.getMappedPort(27017).toString())
            System.setProperty("spring.profiles.active", "mongodb")
        }

        fun cleanup() {
            MONGO_CONTAINER.stop()
            System.clearProperty("mongodb.host")
            System.clearProperty("mongodb.port")
            System.clearProperty("spring.profiles.active")
        }
    }
    override fun setEventPublisher(publisher: EventPublisher) {
        publisher.registerHandlerFor(TestRunStarted::class.java) { _ -> setup() }
        publisher.registerHandlerFor(TestRunFinished::class.java) { _ -> cleanup() }
    }
}
