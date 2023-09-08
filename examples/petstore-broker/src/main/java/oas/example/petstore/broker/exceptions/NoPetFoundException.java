package oas.example.petstore.broker.exceptions;

public class NoPetFoundException extends RuntimeException {
    private final Long id;

    public NoPetFoundException(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
