package io.github.ktakashi.oas.test.cucumber.plugins

import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.Mongod
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess
import de.flapdoodle.reverse.TransitionWalker
import io.cucumber.plugin.EventListener
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestRunStarted

class MongodbPlugin: EventListener {
    companion object {
        lateinit var transitions: TransitionWalker.ReachedState<RunningMongodProcess>

        fun setup() {
            transitions = Mongod.instance().start(Version.Main.V5_0)
            val serverAddress = transitions.current().serverAddress
            System.setProperty("mongodb.host", serverAddress.host)
            System.setProperty("mongodb.port", serverAddress.port.toString())
            System.setProperty("spring.profiles.active", "mongodb")
        }

        fun cleanup() {
            transitions.close()
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
