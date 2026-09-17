package com.securebank.it;

import com.securebank.server.BankServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.nio.file.Files;

/**
 * Shared harness for protocol-level integration tests.
 *
 * Starts a real BankServer on an ephemeral port (port 0) against an isolated
 * data directory (securebank.data.dir is set for the whole test JVM by Surefire),
 * so tests never touch the application's real data files and never collide
 * with a dev server already running on port 8888.
 */
public abstract class ServerHarness {

    protected static BankServer server;
    protected static int port;

    @BeforeAll
    static void startServer() throws Exception {
        // Point persistence at a unique temp dir for this test class run
        File dataDir = Files.createTempDirectory("securebank-it").toFile();
        dataDir.deleteOnExit();
        System.setProperty("securebank.data.dir", dataDir.getAbsolutePath());

        server = new BankServer(0); // 0 = let the OS pick a free port
        Thread serverThread = new Thread(() -> server.start(), "it-bank-server");
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for the socket to be bound
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (server.getBoundPort() > 0) {
                break;
            }
            Thread.sleep(50);
        }
        port = server.getBoundPort();
        if (port <= 0) {
            throw new IllegalStateException("Integration server failed to start");
        }
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    protected static ProtocolClient newClient() throws Exception {
        return new ProtocolClient("localhost", port);
    }
}
