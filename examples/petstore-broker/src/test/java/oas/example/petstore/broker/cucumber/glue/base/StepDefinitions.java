package oas.example.petstore.broker.cucumber.glue.base;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.ktakashi.oas.model.ApiConfiguration;
import io.github.ktakashi.oas.model.ApiData;
import io.github.ktakashi.oas.model.PluginDefinition;
import io.github.ktakashi.oas.test.OasStubTestResources;
import io.github.ktakashi.oas.test.OasStubTestService;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import oas.example.petstore.broker.cucumber.context.TestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StepDefinitions {
    static {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }
    private final Integer localPort;
    private final TestContext testContext = new TestContext();
    private final OasStubTestService oasStubTestService;

    public StepDefinitions(@Value("${local.server.port}") Integer localPort, OasStubTestService oasStubTestService) {
        this.localPort = localPort;
        this.oasStubTestService = oasStubTestService;
    }

    @Given("this pet is not found {long}")
    public void thisPetIsNotFoundId(long id) {
        var config = ApiConfiguration.builder()
                .plugin(PluginDefinition.groovyPlugin(OasStubTestResources.DEFAULT_PLUGIN_SCRIPT))
                .data(ApiData.fromMap(Map.of("/v2/pets/" + id, OasStubTestResources.DefaultResponseModel.builder().status(404).build())))
                .build();
        oasStubTestService.getTestApiContext("petstore")
                .updateApi("/v2/pets/" + id, config)
                .save();
    }

    @When("I get pets")
    public void iGetPets() {
        var uri = UriComponentsBuilder.fromUriString(getApplicationUri())
                .path("/broker/pets")
                .build()
                .toUri();
        testContext.setResponse(given().get(uri));
    }

    @And("I buy a pet of {long}")
    public void iBuyAPetOfId(long id) {
        var uri = UriComponentsBuilder.fromUriString(getApplicationUri())
                .path("/broker/buy/{id}")
                .build(id);
        testContext.setResponse(given().post(uri));
    }

    @Then("I get {int}")
    public void iGetStatus(int status) {
        testContext.getResponse().then().statusCode(status);
    }

    @And("I get order reference of {string}")
    public void iGetOrderReferenceOfReference(String reference) {
        testContext.getResponse().then().body("reference", equalTo(reference));
    }

    @And("I get order ID of {string}")
    public void iGetOrderIDOfRandomUUIDInProd(String id) {
        testContext.getResponse().then().body("id", equalTo(id));
    }

    @And("{string} API {string} is called {int} time(s)")
    public void petstoreAPIVPetsIdIsCalledTime(String context, String api, int count) {
        assertEquals(count, oasStubTestService.getTestApiMetrics(context).byPath(api).count());
        assertEquals(count, oasStubTestService.getTestApiRecords(context).byPath(api).count());
    }

    private String getApplicationUri() {
        return "http://localhost:" + localPort;
    }

}
