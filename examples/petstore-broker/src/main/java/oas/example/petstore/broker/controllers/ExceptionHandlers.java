package oas.example.petstore.broker.controllers;

import oas.example.petstore.broker.exceptions.NoPetFoundException;
import oas.example.petstore.broker.models.PetNotFoundResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionHandlers {

    @ExceptionHandler(NoPetFoundException.class)
    public ResponseEntity<?> noPetFoundException(NoPetFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new PetNotFoundResponse(e.getId()));
    }
}
