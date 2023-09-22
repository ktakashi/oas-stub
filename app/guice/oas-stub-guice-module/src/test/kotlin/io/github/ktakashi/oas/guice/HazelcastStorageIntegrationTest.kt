package io.github.ktakashi.oas.guice

import io.cucumber.junit.platform.engine.Constants
import org.junit.jupiter.api.BeforeAll
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

@Suite
@IncludeEngines(value = ["cucumber"])
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.PLUGIN_PUBLISH_ENABLED_PROPERTY_NAME, value = "true")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "io.github.ktakashi.oas.guice.cucumber.hazelcast,io.github.ktakashi.oas.test.cucumber")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty:build/cucumber/pretty.txt,pretty,io.github.ktakashi.oas.test.cucumber.plugins.HazelcastPlugin")
// No idea how we can let Jersey recognise Guice's method interrupter...
@ConfigurationParameter(key = Constants.FILTER_TAGS_PROPERTY_NAME, value = "not @ignore and not (@delay and @custom)")
class HazelcastStorageIntegrationTest