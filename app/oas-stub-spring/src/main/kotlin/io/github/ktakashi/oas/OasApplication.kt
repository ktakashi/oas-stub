package io.github.ktakashi.oas

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.ConfigurableApplicationContext


@SpringBootApplication(scanBasePackages = [
    "io.github.ktakashi.oas"
])
@ConfigurationPropertiesScan
class OasApplication

fun main(vararg args: String) {
    run(*args)
}

fun run(vararg args: String): ConfigurableApplicationContext = SpringApplication.run(OasApplication::class.java, *args)
