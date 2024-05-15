package io.github.ktakashi.oas.test.glue.https

import io.cucumber.java.BeforeAll
import io.cucumber.spring.CucumberContextConfiguration
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.test.cucumber.TestContext
import io.github.ktakashi.oas.test.cucumber.TestContextSupplier
import io.github.ktakashi.oas.test.cucumber.plugin.OasStubServerPlugin
import io.restassured.RestAssured
import io.restassured.config.SSLConfig
import java.security.KeyStore
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.test.context.ContextConfiguration

@CucumberContextConfiguration
@ContextConfiguration(classes = [SpringBootTestBridge::class])
class SpringBootTestBridge: KoinComponent {
    private val sessionStorage by inject<SessionStorage>()
    private val persistentStorage by inject<PersistentStorage>()

    @Bean @Lazy
    fun sessionStorage() = sessionStorage

    @Bean @Lazy
    fun persistentStorage() = persistentStorage

    @Bean @Lazy
    fun testContextSupplier() = TestContextSupplier {
        val server = OasStubServerPlugin.server
        TestContext("https://localhost:${server.httpsPort()}", server.stubPath(), server.adminPath())
    }
}

@BeforeAll
fun initialize() {
    val server = OasStubServerPlugin.server
    val ks = KeyStore.getInstance("JKS")
    ks.load(null)
    ks.setCertificateEntry("oas-stub", server.certificate())
    val sslConfig = SSLConfig().trustStore(ks)
    RestAssured.config = RestAssured.config().sslConfig(sslConfig)
}
