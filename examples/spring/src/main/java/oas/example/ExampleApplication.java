package oas.example;

import io.github.ktakashi.oas.configuration.ExecutorsProperties;
import io.github.ktakashi.oas.services.DefaultExecutorProvider;
import io.github.ktakashi.oas.web.services.ExecutorProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@ComponentScan("io.github.ktakashi.oas")
public class ExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }

}
