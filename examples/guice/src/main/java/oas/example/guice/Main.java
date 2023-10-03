package oas.example.guice;

import io.github.ktakashi.oas.guice.configurations.OasStubGuiceServerConfiguration;
import io.github.ktakashi.oas.guice.injector.OasStubGuiceInjectors;
import io.github.ktakashi.oas.guice.server.OasStubServer;

public class Main {
    public static void main(String[] args) {
        var configuration = OasStubGuiceServerConfiguration
                .builder()
                .build();
        var injector = OasStubGuiceInjectors.createServerInjector(configuration);
        var server = injector.getInstance(OasStubServer.class);
        server.start();
    }
}
