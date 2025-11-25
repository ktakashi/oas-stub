package io.github.ktakashi.oas.test.glue.https

import io.cucumber.spring.CucumberContextConfiguration
import io.github.ktakashi.oas.server.OasStubServer
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.test.cucumber.TestContext
import io.github.ktakashi.oas.test.cucumber.TestContextSupplier
import io.github.ktakashi.oas.test.cucumber.plugin.OasStubServerPlugin
import io.github.ktakashi.oas.test.ktor.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
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
    fun testContextSupplier(server: OasStubServer) = TestContextSupplier {
        TestContext("https://localhost:${server.httpsPort()}", server.stubPath(), server.adminPath())
    }

    @Bean @Lazy
    fun oasStubServer() = OasStubServerPlugin.server

    @Bean @Lazy
    fun httpClient(server: OasStubServer) = CIO.createHttpClient(server.keyStore())

    private fun OasStubServer.keyStore() = KeyStore.getInstance("JKS").apply {
        load(null)
        setCertificateEntry("oas-stub", certificate())
    }
}
