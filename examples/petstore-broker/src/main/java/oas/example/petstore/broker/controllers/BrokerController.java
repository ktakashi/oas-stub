package oas.example.petstore.broker.controllers;

import oas.example.petstore.broker.models.order.Order;
import oas.example.petstore.broker.models.petstore.Pet;
import oas.example.petstore.broker.services.BrokerService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/broker")
public class BrokerController {
    private final BrokerService brokerService;

    public BrokerController(BrokerService brokerService) {
        this.brokerService = brokerService;
    }

    @GetMapping(path = "/pets", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Pet> getPets() {
        return brokerService.getPets();
    }

    @PostMapping(path = "/buy/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Order> buyPet(@PathVariable("id") Long id) {
        return brokerService.buyPet(id);
    }
}
