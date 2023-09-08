package oas.example.petstore.broker.models;

public class PetNotFoundResponse extends ErrorResponse {
    private final Long id;

    public PetNotFoundResponse(Long id) {
        super("Pet of ID '" + id + "' is not found");
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
