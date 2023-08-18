package io.github.ktakashi.oas

import io.cucumber.core.options.Constants
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite


@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.PLUGIN_PUBLISH_ENABLED_PROPERTY_NAME, value = "true")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "io.github.ktakashi.oas.cucumber.glue")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty:build/cucumber/pretty.txt,pretty,io.github.ktakashi.oas.cucumber.plugins.MongodbPlugin")
@ConfigurationParameter(key = Constants.FILTER_TAGS_PROPERTY_NAME, value = "not @ignore")
class MongodbIntegrationTest
