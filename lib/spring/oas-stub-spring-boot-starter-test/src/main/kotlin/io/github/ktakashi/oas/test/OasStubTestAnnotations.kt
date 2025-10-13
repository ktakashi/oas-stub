package io.github.ktakashi.oas.test

import io.github.ktakashi.oas.test.OasStubTestProperties.Companion.OAS_STUB_SERVER_PROPERTY_PREFIX
import io.github.ktakashi.oas.test.configurations.OasStubServerConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping
import org.springframework.boot.test.autoconfigure.properties.SkipPropertyMapping
import org.springframework.context.annotation.Import

/**
 * Configure OAS Stub server automatically.
 *
 * [port] and [httpsPort] can control the server ports.
 *
 * To do fine-grained configuration, use [OasStubTestProperties]
 */
@Target(allowedTargets = [AnnotationTarget.CLASS])
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(OasStubServerConfiguration::class)
@PropertyMapping(value = OAS_STUB_SERVER_PROPERTY_PREFIX, skip = SkipPropertyMapping.ON_DEFAULT_VALUE)
annotation class AutoConfigureOasStubServer(val port: Int = 0, val httpsPort: Int = -1, val stubConfigurations: Array<String> = [])

/**
 * Convenient annotation to retrieve HTTP port from the OAS Stub server.
 *
 * Equivalent with
 * ```kotlin
 * @Value("${oas.stub.test.server.port}")
 * ```
 */
@Target(allowedTargets = [
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.ANNOTATION_CLASS
])
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Value($$"${$$OAS_STUB_SERVER_PROPERTY_PREFIX.port}")
annotation class OasStubServerPort

/**
 * Convenient annotation to retrieve HTTPS port from the OAS Stub server.
 *
 * Equivalent with
 * ```kotlin
 * @Value("${oas.stub.test.server.https-port}")
 * ```
 */
@Target(allowedTargets = [
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.ANNOTATION_CLASS
])
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Value($$"${$$OAS_STUB_SERVER_PROPERTY_PREFIX.https-port}")
annotation class OasStubServerHttpsPort