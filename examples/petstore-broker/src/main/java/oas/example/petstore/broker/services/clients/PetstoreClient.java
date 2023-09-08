package oas.example.petstore.broker.services.clients;

import oas.example.petstore.broker.configurations.PetstoreBrokerProperties;
import oas.example.petstore.broker.models.petstore.Pet;
import oas.example.petstore.broker.services.ServiceProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class PetstoreClient {
    private static final String PETSTORE_SERVICE_NAME = "petstore";
    private static final String PETS_ENDPOINT = "/v2/pets";
    private static final String PET_ENDPOINT = PETS_ENDPOINT + "/{id}";
    private final WebClient webClient;
    private final ServiceProvider serviceProvider;

    public PetstoreClient(WebClient webClient, ServiceProvider serviceProvider) {
        this.webClient = webClient;
        this.serviceProvider = serviceProvider;
    }

    // Those methods must also check the response status as well as other HTTP headers, if required.
    // But for simplicity and example purpose, we assume that the response always contains a valid
    // JSON
    public Flux<Pet> getPets() {
        var service = serviceProvider.getService(PETSTORE_SERVICE_NAME);
        var uri = UriComponentsBuilder.fromUri(service.url())
                .path(PETS_ENDPOINT)
                .build()
                .toUri();
        return webClient.get().uri(uri).exchangeToFlux(response -> response.bodyToFlux(Pet.class));
    }

    public Mono<Pet> getPet(Long id) {
        var service = serviceProvider.getService(PETSTORE_SERVICE_NAME);
        var uri = UriComponentsBuilder.fromUri(service.url())
                .path(PET_ENDPOINT)
                .build(id);
        return webClient.get().uri(uri).exchangeToMono(response -> response.bodyToMono(Pet.class));
    }
}
