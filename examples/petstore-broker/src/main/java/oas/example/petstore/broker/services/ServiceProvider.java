package oas.example.petstore.broker.services;

import oas.example.petstore.broker.configurations.PetstoreBrokerProperties;
import org.springframework.stereotype.Component;

@Component
public class ServiceProvider {
    private final PetstoreBrokerProperties properties;

    public ServiceProvider(PetstoreBrokerProperties properties) {
        this.properties = properties;
    }

    public PetstoreBrokerProperties.Service getService(String name) {
        var service = properties.getServices().get(name);
        if (service == null) {
            throw new IllegalStateException("Service '" + name + "' is not configured");
        }
        return service;
    }
}
