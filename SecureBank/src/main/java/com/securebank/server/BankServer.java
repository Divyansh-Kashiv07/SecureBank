package com.securebank.server;

import com.securebank.core.*;
import com.securebank.exceptions.*;
import com.securebank.loans.Loan;
import com.securebank.loans.LoanProcessor;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.CustomerRepository;
import com.securebank.transactions.Transaction;
import com.securebank.transactions.TransactionLogger;
import com.securebank.utils.FileIOHelper;
import com.securebank.utils.IDGenerator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * BankServer — the central TCP server that handles all banking operations.
 *
 * RUBRIC COVERAGE:
 * - Unit 4: Socket Programming — uses ServerSocket for TCP server.
 * - Unit 4: Multithreading — spawns a ClientHandler thread per connected client.
 * - Unit 4: Daemon thread — starts TransactionLogger as background logging service.
 * - Unit 1: Command-line argument support — accepts port number from CLI.
 *
 * ARCHITECTURE:
 * The BankServer is the "brain" of the application. It:
 * 1. Opens a ServerSocket on a configurable port
 * 2. Waits for client connections in a loop (accept())
 * 3. For each client, spawns a new ClientHandler thread
 * 4. Holds the master repositories (accounts, customers) — shared across all threads
 * 5. Runs a daemon TransactionLogger for async file logging
 *
 * VIVA NOTE — WHY TCP SOCKETS?
 * TCP (Transmission Control Protocol) guarantees:
 * - Reliable delivery (no lost data)
 * - Ordered delivery (messages arrive in sequence)
 * - Error detection (corrupted data is retransmitted)
 *
 * For banking, this is critical — we CANNOT lose a deposit or withdrawal message.
 * UDP would be faster but unreliable. Imagine a deposit message getting lost!
 *
 * HOW ServerSocket WORKS:
 * 1. ServerSocket binds to a port (e.g., 8888)
 * 2. accept() BLOCKS until a client connects (returns a Socket)
 * 3. The Socket represents the two-way communication channel
 * 4. We hand the Socket to a ClientHandler thread
 * 5. Go back to accept() for the next client
 */
public class BankServer {

    /** The TCP server socket that listens for incoming connections */
    private ServerSocket serverSocket;

    /** Port number to listen on */
    private final int port;

    /** Master repository for all accounts — shared across all client threads */
    private final AccountRepository accountRepository;

    /** Master repository for all customers — shared across all client threads */
    private final CustomerRepository customerRepository;

    /** Loan processing engine */
    private final LoanProcessor loanProcessor;

    /** Daemon thread for asynchronous transaction logging */
    private final TransactionLogger transactionLogger;

    /** Flag to control the server's accept loop */
    private volatile boolean running;

    /** Default port if none specified via CLI */
    public static final int DEFAULT_PORT = 8888;

    /**
     * Creates a new BankServer on the specified port.
     *
     * @param port the TCP port to listen on (e.g., 8888)
     */
    public BankServer(int port) {
        this.port = port;
        this.accountRepository = new AccountRepository();
        this.customerRepository = new CustomerRepository();
        this.loanProcessor = new LoanProcessor();
        this.transactionLogger = new TransactionLogger("data/transaction_log.txt");
        this.running = false;
    }

    /**
     * Starts the bank server:
     * 1. Loads persisted data from files
     * 2. Seeds demo data if empty (first run)
     * 3. Starts the daemon TransactionLogger
     * 4. Opens the ServerSocket
     * 5. Enters the accept loop
     *
     * RUBRIC: Unit 4 — ServerSocket creation and accept loop.
     */
    public void start() {
        try {
            // Step 1: Load data from files
            System.out.println("[Server] Loading data from files...");
            FileIOHelper.ensureDataDirectory();
            accountRepository.loadFromFile();
            customerRepository.loadFromFile();
            loanProcessor.loadFromFile();

            // Step 2: Seed demo data if this is the first run
            if (customerRepository.size() == 0) {
                seedDemoData();
            }

            // Update ID counters based on loaded data
            updateIDCounters();

            // Step 3: Start the daemon transaction logger
            transactionLogger.start();

            // Step 4: Open the ServerSocket
            serverSocket = new ServerSocket(port);
            running = true;

            System.out.println("╔══════════════════════════════════════════════════╗");
            System.out.println("║     SecureBank Server — RUNNING                 ║");
            System.out.println("║     Port: " + port + "                                    ║");
            System.out.println("║     Accounts: " + accountRepository.size() +
                    "  |  Customers: " + customerRepository.size() + "            ║");
            System.out.println("╚══════════════════════════════════════════════════╝");
            System.out.println("[Server] Waiting for client connections...\n");

            // Step 5: Accept loop — runs until server is stopped
            while (running) {
                try {
                    // BLOCKING CALL: waits here until a client connects
                    Socket clientSocket = serverSocket.accept();

                    System.out.println("[Server] New client connected from: " +
                            clientSocket.getInetAddress().getHostAddress());

                    // Create a handler for this client — it implements Runnable
                    ClientHandler handler = new ClientHandler(
                            clientSocket,
                            accountRepository,
                            customerRepository,
                            loanProcessor,
                            transactionLogger
                    );

                    // Spawn a new thread for this client
                    // RUBRIC: Unit 4 — each client gets its own thread
                    Thread clientThread = new Thread(handler,
                            "Client-" + clientSocket.getInetAddress().getHostAddress() +
                                    ":" + clientSocket.getPort());
                    clientThread.start();

                } catch (IOException e) {
                    if (running) {
                        System.err.println("[Server] Error accepting connection: " + e.getMessage());
                    }
                    // If !running, the exception is from serverSocket.close() during shutdown — expected
                }
            }

        } catch (IOException e) {
            System.err.println("[Server] FATAL: Could not start server on port " + port +
                    ": " + e.getMessage());
            System.err.println("[Server] Tip: Is port " + port +
                    " already in use? Try a different port.");
        }
    }

    /**
     * Stops the server gracefully:
     * 1. Sets running flag to false
     * 2. Closes the ServerSocket (unblocks accept())
     * 3. Saves all data to files
     * 4. Stops the transaction logger
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

        // Save all data to files before shutdown
        saveAllData();

        // Stop the daemon logger
        transactionLogger.stop();

        System.out.println("[Server] Server stopped.");
    }

    /**
     * Saves all in-memory data to files (for persistence between restarts).
     */
    public void saveAllData() {
        System.out.println("[Server] Saving all data to files...");
        accountRepository.saveToFile();
        customerRepository.saveToFile();
        loanProcessor.saveToFile();
        System.out.println("[Server] All data saved.");
    }

    /**
     * Seeds demo data for first-run demonstration.
     * Creates sample customers and accounts so the app is immediately usable.
     */
    private void seedDemoData() {
        System.out.println("[Server] First run detected — seeding demo data...");

        // ---- Demo Customer 1: Divyansh Kashiv ----
        Customer customer1 = new Customer(
                "CUSTOMER-1", "Divyansh Kashiv",
                "divyansh@hsbc.com", "9876543210",
                "Greater Noida, UP", "1234"
        );

        String acc1Num = "ACC-001001";
        SavingsAccount savings1 = new SavingsAccount(
                acc1Num, "Divyansh Kashiv", "CUSTOMER-1", 25000.0
        );
        customer1.addAccount(acc1Num);

        String acc2Num = "ACC-001002";
        CurrentAccount current1 = new CurrentAccount(
                acc2Num, "Divyansh Kashiv", "CUSTOMER-1", 50000.0
        );
        customer1.addAccount(acc2Num);

        // ---- Demo Customer 2: Priya Sharma ----
        Customer customer2 = new Customer(
                "CUSTOMER-2", "Priya Sharma",
                "priya@securebank.com", "9876543211",
                "Noida, UP", "5678"
        );

        String acc3Num = "ACC-001003";
        SavingsAccount savings2 = new SavingsAccount(
                acc3Num, "Priya Sharma", "CUSTOMER-2", 15000.0
        );
        customer2.addAccount(acc3Num);

        // ---- Demo Customer 3: Rahul Verma ----
        Customer customer3 = new Customer(
                "CUSTOMER-3", "Rahul Verma",
                "rahul@securebank.com", "9876543212",
                "Delhi, India", "9012"
        );

        String acc4Num = "ACC-001004";
        SavingsAccount savings3 = new SavingsAccount(
                acc4Num, "Rahul Verma", "CUSTOMER-3", 35000.0
        );
        customer3.addAccount(acc4Num);

        // Add to repositories
        customerRepository.addCustomer(customer1);
        customerRepository.addCustomer(customer2);
        customerRepository.addCustomer(customer3);

        accountRepository.addAccount(savings1);
        accountRepository.addAccount(current1);
        accountRepository.addAccount(savings2);
        accountRepository.addAccount(savings3);

        // Save immediately
        saveAllData();

        System.out.println("[Server] Demo data seeded:");
        System.out.println("  Customer: CUSTOMER-1 (Divyansh Kashiv) — PIN: 1234");
        System.out.println("            Accounts: " + acc1Num + " (Savings), " + acc2Num + " (Current)");
        System.out.println("  Customer: CUSTOMER-2 (Priya Sharma) — PIN: 5678");
        System.out.println("            Accounts: " + acc3Num + " (Savings)");
        System.out.println("  Customer: CUSTOMER-3 (Rahul Verma) — PIN: 9012");
        System.out.println("    → Savings: ACC-001004 (₹35,000)");
        System.out.println();
    }

    /**
     * Updates ID generator counters based on loaded data to prevent ID collisions.
     */
    private void updateIDCounters() {
        int maxAcc = 1000, maxCust = 100, maxTxn = 0, maxLoan = 0;

        for (Account acc : accountRepository.getAllAccounts()) {
            maxAcc = Math.max(maxAcc, IDGenerator.extractNumber(acc.getAccountNumber()));
            for (Transaction txn : acc.getTransactionHistory()) {
                maxTxn = Math.max(maxTxn, IDGenerator.extractNumber(txn.getTransactionId()));
            }
        }
        for (Customer cust : customerRepository.getAllCustomers()) {
            maxCust = Math.max(maxCust, IDGenerator.extractNumber(cust.getCustomerId()));
        }
        for (Loan loan : loanProcessor.getAllLoans()) {
            maxLoan = Math.max(maxLoan, IDGenerator.extractNumber(loan.getLoanId()));
        }

        IDGenerator.initializeCounters(maxAcc, maxCust, maxTxn, maxLoan);
    }

    // ==================== GETTERS ====================

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running;
    }

    public AccountRepository getAccountRepository() {
        return accountRepository;
    }

    public CustomerRepository getCustomerRepository() {
        return customerRepository;
    }

    public LoanProcessor getLoanProcessor() {
        return loanProcessor;
    }
}
