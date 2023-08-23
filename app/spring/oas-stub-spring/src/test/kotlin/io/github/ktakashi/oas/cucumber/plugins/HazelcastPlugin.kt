package io.github.ktakashi.oas.cucumber.plugins

import com.hazelcast.cluster.Address
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import io.cucumber.plugin.EventListener
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestRunStarted
import java.lang.IllegalArgumentException

class HazelcastPlugin: EventListener {
    companion object {
        lateinit var hazelcastInstance: HazelcastInstance
    }
    override fun setEventPublisher(publisher: EventPublisher) {
        publisher.registerHandlerFor(TestRunStarted::class.java) { _ -> setup() }
        publisher.registerHandlerFor(TestRunFinished::class.java) { _ -> cleanup() }
    }

    fun setup() {
        hazelcastInstance = Hazelcast.newHazelcastInstance()
        // System.setProperty("hazelcast.cluster.name", hazelcastInstance.name)
        System.setProperty("hazelcast.cluster.members", hazelcastInstance.cluster.members.map { m -> toIpString(m.address) }.joinToString(","))
        System.setProperty("spring.profiles.active", "hazelcast")
        println(System.getProperty("hazelcast.members"))
    }

    private fun toIpString(address: Address) = when {
        address.isIPv4 -> StringBuilder().apply {
            address.inetAddress.address.forEachIndexed { i, b ->
                if (i != 0) append(".")
                append(0xFF and b.toInt())
            }
        }.toString()
        else -> IllegalArgumentException("not supported (yet?)")
    }

    fun cleanup() {
        hazelcastInstance.shutdown()
        System.clearProperty("spring.profiles.active")
    }
}
