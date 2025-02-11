package io.github.ktakashi.oas.gatling

import io.gatling.app.Gatling
import io.gatling.javaapi.core.CoreDsl.global
import io.gatling.javaapi.core.CoreDsl.rampUsersPerSec
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl
import io.gatling.javaapi.http.HttpDsl.status
import io.gatling.javaapi.http.HttpProtocolBuilder
import io.github.ktakashi.oas.server.OasStubServer
import io.github.ktakashi.oas.test.cucumber.plugin.OasStubServerPlugin
import java.time.Duration

class LoadTestSimulation: Simulation() {
    private val server: OasStubServer
    private val http: HttpProtocolBuilder
    private val scn = scenario("Deep nest response object test")
        .exec(HttpDsl.http("[GET] PAIN.002.001.11")
            .get("/oas/pain-api/pain.002.001.11")
            .check(status().`is`(200)))

    init {
        OasStubServerPlugin.setup()
        server = OasStubServerPlugin.server
        http = HttpDsl.http.baseUrl("http://localhost:${server.port()}")

        setUp(scn.injectOpen(rampUsersPerSec(1.0).to(100.0).during(Duration.ofMinutes(1))))
            .protocols(http)
            .assertions(
                global().responseTime().percentile(95.0).lt(200),
                global().responseTime().percentile(99.0).lt(300),
                global().successfulRequests().percent().gte(99.99)
            )
    }

    override fun before() {
        server.resetMetrics()
        server.resetRecords()
    }

    override fun after() {
        OasStubServerPlugin.cleanup()
    }
}

// to run
// JVM_OPTIONS = --add-opens java.base/java.lang=ALL-UNNAMED
// program argument = -rf <directory>
fun main(vararg args: String) {
    Gatling.main(arrayOf("-s", LoadTestSimulation::class.java.name) + args)
}