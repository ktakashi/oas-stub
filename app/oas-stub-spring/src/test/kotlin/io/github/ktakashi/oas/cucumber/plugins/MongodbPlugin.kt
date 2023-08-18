package io.github.ktakashi.oas.cucumber.plugins

import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.Mongod
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess
import de.flapdoodle.reverse.TransitionWalker
import de.flapdoodle.reverse.Transitions
import io.cucumber.plugin.EventListener
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestRunStarted

class MongodbPlugin: EventListener {
    companion object {
        lateinit var transitions: TransitionWalker.ReachedState<RunningMongodProcess>
    }
    override fun setEventPublisher(publisher: EventPublisher) {
        publisher.registerHandlerFor(TestRunStarted::class.java) { _ -> setup() }
        publisher.registerHandlerFor(TestRunFinished::class.java) { _ -> cleanup() }
    }

    private fun setup() {
        transitions = Mongod.instance().start(Version.Main.V5_0)
        val serverAddress = transitions.current().serverAddress
        System.setProperty("mongodb.host", serverAddress.host)
        System.setProperty("mongodb.port", serverAddress.port.toString())
        System.setProperty("spring.profiles.active", "mongodb")
    }

    private fun cleanup() {
        transitions.close()
    }
}
