package oas.example.petstore.broker;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.PLUGIN_PUBLISH_ENABLED_PROPERTY_NAME, value = "true")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "oas.example.petstore.broker.cucumber.glue")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty:target/cucumber/pretty.txt,pretty")
@ConfigurationParameter(key = Constants.FILTER_TAGS_PROPERTY_NAME, value = "not @ignore")
public class IntegrationTest {

}
