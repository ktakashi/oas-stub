package oas.example.petstore.broker.services.clients;

import oas.example.petstore.broker.configurations.PetstoreBrokerProperties;
import oas.example.petstore.broker.models.order.NewOrder;
import oas.example.petstore.broker.models.order.Order;
import oas.example.petstore.broker.services.ServiceProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Component
public class OrderClient {
    public static final String ORDER_SERVICE_NAME = "order";
    private final WebClient webClient;
    private final PetstoreBrokerProperties.Service service;

    public OrderClient(WebClient webClient, ServiceProvider serviceProvider) {
        this.webClient = webClient;
        this.service = serviceProvider.getService(ORDER_SERVICE_NAME);
    }

    public Mono<Order> newOrder(NewOrder newOrder) {
        var uri = UriComponentsBuilder.fromUri(service.url()).path("/v1/order").build().toUri();
        return webClient.post().uri(uri)
                .body(BodyInserters.fromValue(newOrder))
                .exchangeToMono(response -> response.bodyToMono(Order.class));
    }
}
