package oas.example.petstore.broker.configurations;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(PetstoreBrokerProperties.class)
public class PetstoreBrokerConfiguration {
    @Bean
    public WebClient webClient() {
        // For this example application, we don't do much about it
        return WebClient.builder().build();
    }
}
