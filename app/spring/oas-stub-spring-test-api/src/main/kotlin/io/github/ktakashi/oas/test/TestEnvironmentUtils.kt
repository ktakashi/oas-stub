@file:JvmName("TestEnvironmentUtils")
package io.github.ktakashi.oas.test

import org.springframework.core.env.ConfigurableEnvironment

fun isPortShared(environment: ConfigurableEnvironment) =
    environment.getProperty("${OasStubTestProperties.OAS_STUB_TEST_PROPERTY_PREFIX}.server.shared")?.toBoolean() ?: true

