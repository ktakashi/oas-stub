package io.github.ktakashi.oas.test

import io.github.ktakashi.oas.test.OasStubTestProperties.Companion.OAS_STUB_SERVER_PROPERTY_PREFIX
import io.github.ktakashi.oas.test.configurations.OasStubServerConfiguration
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping
import org.springframework.boot.test.autoconfigure.properties.SkipPropertyMapping
import org.springframework.context.annotation.Import

@Target(allowedTargets = [AnnotationTarget.CLASS])
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(OasStubServerConfiguration::class)
@PropertyMapping(value = OAS_STUB_SERVER_PROPERTY_PREFIX, skip = SkipPropertyMapping.ON_DEFAULT_VALUE)
annotation class AutoConfigureOasStubServer(val port: Int = 0, val httpsPort: Int = -1)