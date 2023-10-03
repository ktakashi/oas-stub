package io.github.ktakashi.oas.test

import io.github.ktakashi.oas.test.configurations.OasStubTestConfiguration
import org.springframework.context.annotation.Import

@Target(allowedTargets = [AnnotationTarget.CLASS])
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(OasStubTestConfiguration::class)
annotation class AutoConfigureOasStub