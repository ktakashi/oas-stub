package oas.example.petstore.broker.cucumber.glue.spring;

import io.cucumber.spring.CucumberContextConfiguration;
import io.github.ktakashi.oas.test.AutoConfigureOasStub;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
@AutoConfigureOasStub // This is needed to enable OAS stub
public class SpringSteps {
}
