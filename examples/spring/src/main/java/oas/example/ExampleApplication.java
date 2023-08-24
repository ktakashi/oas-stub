package oas.example;

import io.github.ktakashi.oas.OasApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(new Class[] { ExampleApplication.class, OasApplication.class }, args);
    }
}
