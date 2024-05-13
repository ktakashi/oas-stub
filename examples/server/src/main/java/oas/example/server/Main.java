package oas.example.server;

import io.github.ktakashi.oas.server.OasStubServer;
import io.github.ktakashi.oas.server.options.OasStubOptions;

public class Main {
    public static void main(String[] args) throws Exception {
        var options = OasStubOptions.builder().build();
        new OasStubServer(options).start();
        Thread.currentThread().join(); // infinite loop
    }
}
