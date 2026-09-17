package com.securebank.server;

import com.securebank.loans.LoanProcessor;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.BeneficiaryRepository;
import com.securebank.repository.CustomerRepository;
import com.securebank.service.BankService;
import com.securebank.transactions.TransactionLogger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * BankServer — the TCP front door of the bank.
 *
 * The server owns the socket lifecycle and nothing else:
 * 1. Opens a ServerSocket on the configured port.
 * 2. Blocks in accept() waiting for clients.
 * 3. Spawns one {@link ClientHandler} thread per connection (so several
 *    customers are served concurrently).
 * 4. Wires the shared repositories + transaction log daemon into a single
 *    {@link BankService} instance that every handler shares.
 * 5. Shuts down gracefully, flushing all data to disk.
 *
 * Every banking rule, authorization decision and persistence call lives in
 * BankService — the server never touches account state directly.
 *
 * WHY TCP: a deposit or withdrawal must never be lost or reordered, so the
 * protocol needs TCP's reliable, ordered delivery.
 */
public class BankServer {

    /** Default port if none is specified on the command line. */
    public static final int DEFAULT_PORT = 8888;

    /** The TCP server socket that listens for incoming connections. */
    private volatile ServerSocket serverSocket;

    /** Port number requested on construction (0 = let the OS pick a free one). */
    private final int port;

    /** The actually bound port — only differs from {@link #port} when port 0 was used. */
    private volatile int boundPort;

    /** The application service shared by every client handler. */
    private final BankService bankService;

    /** Daemon thread that writes the transaction log asynchronously. */
    private final TransactionLogger transactionLogger;

    /** Flag controlling the accept loop. */
    private volatile boolean running;

    /**
     * Creates a server and its object graph (repositories, logger, service).
     *
     * @param port the TCP port to listen on (0 = ephemeral, used by tests)
     */
    public BankServer(int port) {
        this.port = port;

        AccountRepository accountRepository = new AccountRepository();
        CustomerRepository customerRepository = new CustomerRepository();
        BeneficiaryRepository beneficiaryRepository = new BeneficiaryRepository();
        LoanProcessor loanProcessor = new LoanProcessor();

        // Respect the configurable data directory so tests never write into the real data dir
        String dataDir = System.getProperty("securebank.data.dir", "data");
        this.transactionLogger = new TransactionLogger(
                dataDir + File.separator + "transaction_log.txt");

        this.bankService = new BankService(accountRepository, customerRepository,
                loanProcessor, beneficiaryRepository, transactionLogger);
    }

    /**
     * Loads persisted state, opens the socket and serves clients until stopped.
     */
    public void start() {
        try {
            System.out.println("[Server] Loading data from files...");
            bankService.loadAll();

            if (bankService.isEmpty()) {
                bankService.seedDemoData();
            }
            bankService.updateIdCounters();
            bankService.startLogger();

            // Port 0 lets the OS assign an ephemeral port (integration tests)
            serverSocket = new ServerSocket(port);
            boundPort = serverSocket.getLocalPort();
            running = true;

            printBanner();
            System.out.println("[Server] Waiting for client connections...\n");

            while (running) {
                try {
                    // BLOCKING CALL: waits here until a client connects
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[Server] New client connected from: "
                            + clientSocket.getInetAddress().getHostAddress());

                    Thread clientThread = new Thread(new ClientHandler(clientSocket, bankService),
                            "Client-" + clientSocket.getInetAddress().getHostAddress()
                                    + ":" + clientSocket.getPort());
                    clientThread.start();

                } catch (IOException e) {
                    if (running) {
                        System.err.println("[Server] Error accepting connection: " + e.getMessage());
                    }
                    // When !running the exception comes from serverSocket.close() during
                    // shutdown and is expected
                }
            }

        } catch (IOException e) {
            System.err.println("[Server] FATAL: Could not start server on port " + port
                    + ": " + e.getMessage());
            System.err.println("[Server] Tip: Is port " + port
                    + " already in use? Try a different port.");
        }
    }

    /**
     * Stops the server gracefully: stops accepting, saves everything, drains
     * and stops the transaction logger.
     */
    public void stop() {
        System.out.println("[Server] Shutting down...");
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[Server] Error closing server socket: " + e.getMessage());
        }

        saveAllData();
        transactionLogger.stop();

        System.out.println("[Server] Server stopped.");
    }

    /**
     * Persists all in-memory data.
     *
     * RELIABILITY: failures are logged CRITICALLY but not rethrown — this runs
     * from shutdown hooks and auto-save paths where there is no caller left to
     * report to, and the atomic-save design means a failure leaves the previous
     * data files intact rather than corrupting them.
     */
    public void saveAllData() {
        System.out.println("[Server] Saving all data to files...");
        try {
            bankService.saveAll();
            System.out.println("[Server] All data saved.");
        } catch (IOException e) {
            System.err.println("[Server] CRITICAL: failed to persist data: " + e.getMessage());
        }
    }

    /** Prints the startup banner with the live port and data counts. */
    private void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║     SecureBank Server — RUNNING                  ║");
        System.out.println("║     Port: " + boundPort);
        System.out.println("║     Accounts: " + bankService.getAccountCount()
                + "  |  Customers: " + bankService.getCustomerCount());
        System.out.println("╚══════════════════════════════════════════════════╝");
    }

    // ==================== STATE ====================

    /** @return the port the server was asked to bind */
    public int getPort() {
        return port;
    }

    /**
     * @return the port the server actually bound. Differs from {@link #getPort()}
     *         only when the server was started with port 0.
     */
    public synchronized int getBoundPort() {
        return boundPort > 0 ? boundPort : port;
    }

    /** @return true while the accept loop is running */
    public boolean isRunning() {
        return running;
    }
}
