package com.securebank.client;

import com.securebank.server.BankServer;

import java.io.*;
import java.net.Socket;
import java.net.ConnectException;

/**
 * BankClient — manages the TCP socket connection from the Swing GUI to the BankServer.
 *
 * RUBRIC COVERAGE:
 * - Unit 4: Socket Programming — uses Socket for TCP client connection.
 * - Unit 4: I/O Streams — BufferedReader/PrintWriter over socket streams.
 *
 * This class is used by all GUI panels to send requests and receive responses.
 * Each GUI action (deposit, withdraw, transfer, etc.) calls a method here,
 * which sends a formatted command over the socket and returns the parsed response.
 *
 * THREAD SAFETY NOTE:
 * GUI actions run on the Event Dispatch Thread (EDT). Socket I/O is blocking.
 * To avoid freezing the GUI, all socket calls should be made from SwingWorker
 * background threads (handled by the GUI panels, not here).
 * However, this class itself synchronizes its send/receive to prevent interleaving
 * if multiple SwingWorkers somehow call it simultaneously.
 *
 * VIVA NOTE — Socket vs ServerSocket:
 * - ServerSocket: Server-side. Listens for connections on a port. accept() returns Sockets.
 * - Socket: Client-side. Connects to a server's IP:port. Provides I/O streams for communication.
 * The Socket is a TWO-WAY channel — you can both read and write through it.
 */
public class BankClient {

    /** The TCP socket connection to the server */
    private Socket socket;

    /** Writer for sending requests to the server */
    private PrintWriter out;

    /** Reader for receiving responses from the server */
    private BufferedReader in;

    /** Server hostname (default: localhost for same-machine testing) */
    private final String host;

    /** Server port */
    private final int port;

    /** Whether we're currently connected */
    private boolean connected;

    /**
     * Creates a BankClient configured to connect to the specified server.
     *
     * @param host the server hostname or IP address
     * @param port the server port number
     */
    public BankClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.connected = false;
    }

    /**
     * Convenience constructor for connecting to localhost.
     *
     * @param port the server port
     */
    public BankClient(int port) {
        this("localhost", port);
    }

    /**
     * Default constructor — connects to localhost:8888.
     */
    public BankClient() {
        this("localhost", BankServer.DEFAULT_PORT);
    }

    // ==================== CONNECTION MANAGEMENT ====================

    /**
     * Establishes a TCP connection to the BankServer.
     *
     * RUBRIC: Unit 4 — Socket creation and stream setup.
     *
     * @return true if connection was successful
     */
    public boolean connect() {
        try {
            // Create the socket — this initiates the TCP handshake
            socket = new Socket(host, port);

            // Set up I/O streams (Character Streams over the socket's byte streams)
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            connected = true;
            System.out.println("[Client] Connected to server at " + host + ":" + port);
            return true;

        } catch (ConnectException e) {
            System.err.println("[Client] Connection refused. Is the server running on " +
                    host + ":" + port + "?");
            return false;
        } catch (IOException e) {
            System.err.println("[Client] Connection failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Disconnects from the server and closes all resources.
     */
    public void disconnect() {
        try {
            connected = false;
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("[Client] Disconnected from server.");
        } catch (IOException e) {
            System.err.println("[Client] Error during disconnect: " + e.getMessage());
        }
    }

    /**
     * Sends a raw request to the server and receives the response.
     * This is the core communication method — all other methods use this.
     *
     * SYNCHRONIZED to prevent interleaving if called from multiple threads.
     *
     * @param request the formatted request string (e.g., "DEPOSIT|ACC-001001|5000")
     * @return the server's response string, or null if communication failed
     */
    public synchronized String sendRequest(String request) {
        if (!connected || socket == null || socket.isClosed()) {
            System.err.println("[Client] Not connected to server.");
            return null;
        }

        try {
            // Send the request
            out.println(request);

            // Wait for and return the response
            String response = in.readLine();
            return response;

        } catch (IOException e) {
            System.err.println("[Client] Communication error: " + e.getMessage());
            connected = false;
            return null;
        }
    }

    // ==================== HIGH-LEVEL BANKING OPERATIONS ====================
    // These methods provide a clean API for the GUI panels.
    // Each method formats the request, sends it, and returns the raw response.

    /**
     * Authenticates a customer with their ID and PIN.
     *
     * @param customerId the customer's ID
     * @param pin        the customer's PIN
     * @return server response: "OK|name|accounts" or "ERROR|message"
     */
    public String authenticate(String customerId, String pin) {
        return sendRequest("LOGIN|" + customerId + "|" + pin);
    }

    /**
     * Gets the balance of an account.
     *
     * @param accountNumber the account to query
     * @return server response: "OK|balance" or "ERROR|message"
     */
    public String getBalance(String accountNumber) {
        return sendRequest("BALANCE|" + accountNumber);
    }

    /**
     * Deposits money into an account.
     *
     * @param accountNumber the target account
     * @param amount        the amount to deposit
     * @param remarks       optional remarks
     * @return server response: "OK|newBalance|txnId" or "ERROR|message"
     */
    public String deposit(String accountNumber, double amount, String remarks) {
        return sendRequest("DEPOSIT|" + accountNumber + "|" +
                String.format("%.2f", amount) + "|" + remarks);
    }

    /**
     * Withdraws money from an account.
     *
     * @param accountNumber the source account
     * @param amount        the amount to withdraw
     * @return server response: "OK|newBalance|txnId" or "ERROR|message"
     */
    public String withdraw(String accountNumber, double amount) {
        return sendRequest("WITHDRAW|" + accountNumber + "|" + String.format("%.2f", amount));
    }

    /**
     * Transfers money between accounts.
     *
     * @param fromAccount the source account
     * @param toAccount   the destination account
     * @param amount      the amount to transfer
     * @return server response: "OK|newBalance|txnId" or "ERROR|message"
     */
    public String transfer(String fromAccount, String toAccount, double amount) {
        return sendRequest("TRANSFER|" + fromAccount + "|" + toAccount + "|" +
                String.format("%.2f", amount));
    }

    /**
     * Gets the transaction history for an account.
     *
     * @param accountNumber the account to query
     * @return server response: "OK|txn1;txn2;..." or "OK|EMPTY"
     */
    public String getTransactionHistory(String accountNumber) {
        return sendRequest("HISTORY|" + accountNumber);
    }

    /**
     * Gets account information (holder name, type, balance, interest rate).
     *
     * @param accountNumber the account to query
     * @return server response: "OK|name|type|balance|rate" or "ERROR|message"
     */
    public String getAccountInfo(String accountNumber) {
        return sendRequest("ACCOUNT_INFO|" + accountNumber);
    }

    /**
     * Applies for a loan.
     *
     * @param customerId    the customer applying
     * @param accountNumber the linked account
     * @param amount        the loan amount
     * @param tenureMonths  the loan duration
     * @param purpose       the reason for the loan
     * @return server response: "OK|loanId|emi" or "ERROR|message"
     */
    public String applyForLoan(String customerId, String accountNumber,
                                double amount, int tenureMonths, String purpose) {
        return sendRequest("LOAN_APPLY|" + customerId + "|" + accountNumber + "|" +
                String.format("%.2f", amount) + "|" + tenureMonths + "|" + purpose);
    }

    /**
     * Gets loan status for a customer.
     *
     * @param customerId the customer to query
     * @return server response: "OK|loan1;loan2;..." or "OK|EMPTY"
     */
    public String getLoanStatus(String customerId) {
        return sendRequest("LOAN_STATUS|" + customerId);
    }

    /**
     * Gets all accounts for a customer.
     *
     * @param customerId the customer to query
     * @return server response: "OK|acc1;acc2;..." or "OK|EMPTY"
     */
    public String getAccounts(String customerId) {
        return sendRequest("ACCOUNTS|" + customerId);
    }

    /**
     * Creates a new account for a customer.
     *
     * @param customerId     the customer
     * @param accountType    "Savings" or "Current"
     * @param initialBalance the starting balance
     * @return server response: "OK|accountNumber" or "ERROR|message"
     */
    public String createAccount(String customerId, String accountType, double initialBalance) {
        return sendRequest("CREATE_ACCOUNT|" + customerId + "|" + accountType + "|" +
                String.format("%.2f", initialBalance));
    }

    /**
     * Gets the calculated interest for an account.
     *
     * @param accountNumber the account to query
     * @return server response: "OK|interestAmount" or "ERROR|message"
     */
    public String getInterest(String accountNumber) {
        return sendRequest("INTEREST|" + accountNumber);
    }

    // ==================== STATE QUERIES ====================

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
