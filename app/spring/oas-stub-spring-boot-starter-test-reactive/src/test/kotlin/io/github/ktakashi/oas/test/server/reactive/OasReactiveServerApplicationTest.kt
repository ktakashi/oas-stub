package io.github.ktakashi.oas.test.server.reactive

import io.github.ktakashi.oas.model.ApiData
import io.github.ktakashi.oas.model.ApiHeaders
import io.github.ktakashi.oas.test.OasStubTestResources
import io.github.ktakashi.oas.test.OasStubTestService
import io.restassured.RestAssured
import io.restassured.RestAssured.config
import io.restassured.RestAssured.given
import io.restassured.config.SSLConfig
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import java.math.BigInteger
import java.net.URI
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@SpringBootApplication
class OasServerApplication

@SpringBootTest
@AutoConfigureOasStubReactiveServer(port = 0, httpsPort = 0)
class OasReactiveServerApplicationTest(@Autowired private val oasStubTestService: OasStubTestService,
                                       @Value("\${oas.stub.test.server.port}") private val httpPort: Int,
                                       @Value("\${oas.stub.test.server.https-port}") private val httpsPort: Int) {

    @TestConfiguration
    class TestSecurityConfig {
        @Bean
        fun certificate() = createCertificate()
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            config = config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation().allowAllHostnames())
        }
    }

    @Test
    fun test() {
        assertTrue(httpPort > 0)
        assertTrue(httpsPort > 0)

        check("http", httpPort, oasStubTestService)
    }

    @Test
    fun testHttps() {
        assertTrue(httpPort > 0)
        assertTrue(httpsPort > 0)

        check("https", httpsPort, oasStubTestService)
    }
}

private fun check(scheme: String, httpPort: Int, oasStubTestService: OasStubTestService) {
    RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())

    given().get(URI.create("${scheme}://localhost:$httpPort/oas/petstore/v1/pets/1"))
        .then()
        .statusCode(200)
        .body("id", equalTo(1))
    assertEquals(1, oasStubTestService.getTestApiMetrics("petstore").byPath("/v1/pets/1").count())

    given().get(URI.create("${scheme}://localhost:$httpPort/oas/petstore/v1/pets/2"))
        .then()
        .statusCode(404)
        .body("message", equalTo("No pet found"))
    assertEquals(1, oasStubTestService.getTestApiMetrics("petstore").byStatus(200).count())
    assertEquals(1, oasStubTestService.getTestApiMetrics("petstore").byStatus(404).count())
    assertEquals(2, oasStubTestService.getTestApiMetrics("petstore").filter { m -> m.httpMethod == "GET"}.count())

    val context = oasStubTestService.getTestApiContext("petstore")
        .updateHeaders(ApiHeaders(response = sortedMapOf("Extra-Header" to listOf("extra-value"))))
    context.getApiConfiguration("/v1/pets/{id}").ifPresent { config ->
        val value = OasStubTestResources.DefaultResponseModel(status = 200, response = """{"id": 2,"name": "Pochi","tag": "dog"}""")
        val map = config.data?.asMap()?.toMutableMap()
        map?.put("/v1/pets/2", value)
        context.updateApi("/v1/pets/{id}", config.updateData(ApiData(map!!))).save()
    }

    given().get(URI.create("${scheme}://localhost:$httpPort/oas/petstore/v1/pets/2"))
        .then()
        .statusCode(200)
        .header("Extra-Header", equalTo("extra-value"))
        .body("id", equalTo(2))
}

private fun makeTestKeyStore(certificate: Triple<String, PrivateKey, X509Certificate>, password: String, type: String = "PKCS12") = KeyStore.getInstance(type).also { ks ->
    val passCharArray = password.toCharArray()
    ks.load(null, passCharArray)
    ks.setKeyEntry(certificate.first, certificate.second, passCharArray, arrayOf(certificate.third))
}

private fun createCertificate(): Pair<PrivateKey, X509Certificate> {
    val issuer = X500Name("CN=localhost")
    val spec = ECGenParameterSpec("secp256r1")
    val kpg = KeyPairGenerator.getInstance("EC").apply {
        initialize(spec)
    }
    val kp = kpg.generateKeyPair()
    val now = System.currentTimeMillis()
    val spki = SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(kp.public.encoded))
    val cg = X509v3CertificateBuilder(issuer, BigInteger.ONE, Date(now), Date(now + 1000 * 60 * 24 * 365), issuer, spki)
    val signer = JcaContentSignerBuilder("SHA256withECDSA").build(kp.private)
    return kp.private to JcaX509CertificateConverter().getCertificate(cg.build(signer))
}
