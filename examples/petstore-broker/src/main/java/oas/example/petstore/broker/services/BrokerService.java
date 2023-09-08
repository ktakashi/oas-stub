package oas.example.petstore.broker.services;

import oas.example.petstore.broker.models.order.NewOrder;
import oas.example.petstore.broker.models.order.Order;
import oas.example.petstore.broker.models.petstore.Pet;
import oas.example.petstore.broker.services.clients.OrderClient;
import oas.example.petstore.broker.services.clients.PetstoreClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class BrokerService {
    private final PetstoreClient petstoreClient;
    private final OrderClient orderClient;

    public BrokerService(PetstoreClient petstoreClient, OrderClient orderClient) {
        this.petstoreClient = petstoreClient;
        this.orderClient = orderClient;
    }

    public Flux<Pet> getPets() {
        return petstoreClient.getPets();
    }

    public Mono<Order> buyPet(Long id) {
        return petstoreClient.getPet(id).flatMap(pet -> orderClient.newOrder(new NewOrder(toReference(pet))));
    }

    private String toReference(Pet pet) {
        return "id-" + pet.getId() + ",name-" + pet.getName();
    }
}
