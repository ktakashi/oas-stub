package io.github.ktakashi.oas.test.server.reactive

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping
import org.springframework.boot.test.autoconfigure.properties.SkipPropertyMapping
import org.springframework.context.annotation.Import

@Target(allowedTargets = [AnnotationTarget.CLASS])
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(OasStubReactiveConfiguration::class)
@EnableAutoConfiguration
@PropertyMapping(value = OAS_STUB_REACTIVE_SERVER_PROPERTY_PREFIX, skip = SkipPropertyMapping.ON_DEFAULT_VALUE)
annotation class AutoConfigureOasStubReactiveServer(val port: Int = 0, val httpsPort: Int = -1)