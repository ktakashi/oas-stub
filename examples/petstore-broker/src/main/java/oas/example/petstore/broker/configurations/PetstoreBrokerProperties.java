package oas.example.petstore.broker.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("petstore.broker")
public class PetstoreBrokerProperties {
    private Map<String, Service> services = new HashMap<>();

    public Map<String, Service> getServices() {
        return services;
    }

    public void setServices(Map<String, Service> services) {
        this.services = services;
    }

    public record Service(URI url) {}
}
