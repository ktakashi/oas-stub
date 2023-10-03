package oas.example.petstore.broker.cucumber.glue.guice;

import io.cucumber.spring.CucumberContextConfiguration;
import io.github.ktakashi.oas.test.server.AutoConfigureOasStubServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "guice"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
@AutoConfigureOasStubServer
public class GuiceSteps {
}
