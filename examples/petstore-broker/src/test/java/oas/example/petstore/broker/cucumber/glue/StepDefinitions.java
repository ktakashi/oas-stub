package oas.example.petstore.broker.cucumber.glue;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import oas.example.petstore.broker.cucumber.context.TestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
@EnableAutoConfiguration // This is needed to enable OAS stub
public class StepDefinitions {
    static {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }
    private final Integer localPort;
    private final TestContext testContext = new TestContext();

    public StepDefinitions(@Value("${local.server.port}") Integer localPort) {
        this.localPort = localPort;
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

    private String getApplicationUri() {
        return "http://localhost:" + localPort;
    }

}
