package com.securebank.server;

import com.securebank.core.Customer;
import com.securebank.service.BankService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * ClientHandler — the transport layer for one connected client.
 *
 * It does exactly four things and nothing else:
 * 1. FRAMING — reads newline-delimited requests and writes one response line each.
 * 2. TRANSPORT GUARDS — caps request size, logs with PINs masked, closes cleanly.
 * 3. CONNECTION SECURITY — requires LOGIN before any other command, and applies
 *    the per-connection failure lockout (see {@link Session}).
 * 4. DISPATCH — maps a command name plus its arguments onto {@link BankService}.
 *
 * All banking rules and every authorization decision live in BankService, so
 * this class has no knowledge of accounts, balances or persistence.
 *
 * PROTOCOL
 *   request   COMMAND|param1|param2|...
 *   response  OK|data...   or   ERROR|message
 *
 * COMMANDS
 *   LOGIN|customerId|pin                     → OK|customerName|accountNumbers
 *   BALANCE|accountNumber                    → OK|balance
 *   DEPOSIT|accountNumber|amount[|remarks]   → OK|newBalance|txnId
 *   WITHDRAW|accountNumber|amount            → OK|newBalance|txnId
 *   TRANSFER|fromAccount|toAccount|amount    → OK|newBalance|txnId
 *   HISTORY|accountNumber                    → OK|txn1;txn2;... | OK|EMPTY
 *   ACCOUNT_INFO|accountNumber               → OK|holder|type|balance|rate
 *   INTEREST|accountNumber                   → OK|interest
 *   ACCOUNTS|customerId                      → OK|num,type,balance;... | OK|EMPTY
 *   CREATE_ACCOUNT|type|initialBalance       → OK|accountNumber
 *   LOAN_APPLY|custId|accNum|amount|tenure|purpose → OK|loanId|emi
 *   LOAN_STATUS|customerId                   → OK|loan1;loan2;... | OK|EMPTY
 *   BENEFICIARY_LIST                         → OK|id|name|acc|bank|nick;... | OK|EMPTY
 *   BENEFICIARY_ADD|name|acc|bank|nickname   → OK|beneficiaryId
 *   BENEFICIARY_REMOVE|beneficiaryId         → OK|message
 *   QUIT                                     → BYE (then the socket closes)
 *
 * SECURITY NOTES
 * - SAVE is a server-side maintenance command: it is rejected for clients so a
 *   connection cannot force disk writes.
 * - Internal failures are logged server-side and answered with a generic
 *   message, so implementation details never leak to a client.
 */
public class ClientHandler implements Runnable {

    /** SECURITY: maximum accepted request length in characters. */
    private static final int MAX_REQUEST_LENGTH = 256;

    /** The socket connection to this specific client. */
    private final Socket clientSocket;

    /** Application layer — owns every banking rule and authorization check. */
    private final BankService bankService;

    /** Per-connection authentication/authorization state. */
    private final Session session = new Session();

    /** Reader for incoming client messages. */
    private BufferedReader in;

    /** Writer for outgoing server responses. */
    private PrintWriter out;

    /**
     * Creates a handler for one client connection.
     *
     * @param clientSocket the client's socket connection
     * @param bankService  the shared application service
     */
    public ClientHandler(Socket clientSocket, BankService bankService) {
        this.clientSocket = clientSocket;
        this.bankService = bankService;
    }

    /**
     * Serves this connection until the client disconnects or sends QUIT.
     */
    @Override
    public void run() {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        System.out.println("[Handler] Thread started for client: " + clientAddress);

        try {
            // UTF-8 explicitly: the platform default (windows-1252) cannot encode ₹/Devanagari
            in = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(
                    clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

            String requestLine;
            while ((requestLine = in.readLine()) != null) {
                requestLine = requestLine.trim();
                if (requestLine.isEmpty()) continue;

                if (requestLine.length() > MAX_REQUEST_LENGTH) {
                    System.out.println("[Handler " + clientAddress
                            + "] REJECTED oversized request (" + requestLine.length() + " chars)");
                    out.println("ERROR|Request too long");
                    continue;
                }

                // SECURITY: never log PINs
                boolean isLogin = requestLine.regionMatches(true, 0, "LOGIN|", 0, 6);
                System.out.println("[Handler " + clientAddress + "] Request: "
                        + (isLogin ? "LOGIN|customerId|****" : requestLine));

                String response = processCommand(requestLine);
                out.println(response);

                if ("BYE".equals(response)) {
                    break;
                }
                System.out.println("[Handler " + clientAddress + "] Response: " + response);
            }

        } catch (IOException e) {
            System.out.println("[Handler " + clientAddress + "] Client disconnected: " + e.getMessage());
        } finally {
            cleanup();
            System.out.println("[Handler " + clientAddress + "] Thread finished.");
        }
    }

    /**
     * Applies the connection-level security gate and routes the request.
     *
     * @param request the raw request line
     * @return the response line to send back
     */
    private String processCommand(String request) {
        String[] parts = request.split("\\|", -1);
        String command = parts[0].toUpperCase();

        // SECURITY: LOGIN and QUIT are the only commands allowed before login
        if (!session.isAuthenticated() && !"LOGIN".equals(command) && !"QUIT".equals(command)) {
            return "ERROR|Authentication required. LOGIN first.";
        }

        try {
            return dispatch(command, parts);
        } catch (Exception e) {
            // SECURITY: details stay server-side; the client gets a generic message
            System.err.println("[Handler] Internal error: " + e);
            return "ERROR|Internal server error";
        }
    }

    /**
     * Maps a command name onto the matching service call.
     */
    private String dispatch(String command, String[] parts) {
        String customerId = session.getCustomerId();

        return switch (command) {
            case "LOGIN" -> login(parts);
            case "QUIT" -> "BYE";

            case "BALANCE" -> parts.length < 2
                    ? "ERROR|Usage: BALANCE|accountNumber"
                    : bankService.getBalance(customerId, parts[1]);

            case "DEPOSIT" -> parts.length < 3
                    ? "ERROR|Usage: DEPOSIT|accountNumber|amount[|remarks]"
                    : bankService.deposit(customerId, parts[1], parts[2],
                            parts.length > 3 ? parts[3] : "");

            case "WITHDRAW" -> parts.length < 3
                    ? "ERROR|Usage: WITHDRAW|accountNumber|amount"
                    : bankService.withdraw(customerId, parts[1], parts[2]);

            case "TRANSFER" -> parts.length < 4
                    ? "ERROR|Usage: TRANSFER|fromAccount|toAccount|amount"
                    : bankService.transfer(customerId, parts[1], parts[2], parts[3]);

            case "HISTORY" -> parts.length < 2
                    ? "ERROR|Usage: HISTORY|accountNumber"
                    : bankService.getHistory(customerId, parts[1]);

            case "ACCOUNT_INFO" -> parts.length < 2
                    ? "ERROR|Usage: ACCOUNT_INFO|accountNumber"
                    : bankService.getAccountInfo(customerId, parts[1]);

            case "INTEREST" -> parts.length < 2
                    ? "ERROR|Usage: INTEREST|accountNumber"
                    : bankService.calculateInterest(customerId, parts[1]);

            case "ACCOUNTS" -> parts.length < 2
                    ? "ERROR|Usage: ACCOUNTS|customerId"
                    : bankService.getAccounts(customerId, parts[1]);

            case "CREATE_ACCOUNT" -> parts.length < 3
                    ? "ERROR|Usage: CREATE_ACCOUNT|type|initBalance"
                    // Accepts both CREATE_ACCOUNT|custId|type|balance and CREATE_ACCOUNT|type|balance:
                    // the customer is always taken from the session, never from the request
                    : bankService.createAccount(customerId,
                            parts[parts.length - 2], parts[parts.length - 1]);

            case "LOAN_APPLY" -> parts.length < 6
                    ? "ERROR|Usage: LOAN_APPLY|custId|accNum|amount|tenure|purpose"
                    : bankService.applyForLoan(customerId, parts[1], parts[2], parts[3],
                            parts[4], parts[5]);

            case "LOAN_STATUS" -> parts.length < 2
                    ? "ERROR|Usage: LOAN_STATUS|customerId"
                    : bankService.getLoans(customerId, parts[1]);

            case "BENEFICIARY_LIST" -> bankService.getBeneficiaries(customerId);

            case "BENEFICIARY_ADD" -> parts.length < 5
                    ? "ERROR|Usage: BENEFICIARY_ADD|name|accountNumber|bank|nickname"
                    : bankService.addBeneficiary(customerId, parts[1], parts[2], parts[3], parts[4]);

            case "BENEFICIARY_REMOVE" -> parts.length < 2
                    ? "ERROR|Usage: BENEFICIARY_REMOVE|beneficiaryId"
                    : bankService.removeBeneficiary(customerId, parts[1]);

            // SECURITY: server-side maintenance command — never exposed to clients
            case "SAVE" -> "ERROR|Not authorized";

            default -> "ERROR|Unknown command: " + command;
        };
    }

    /**
     * Authenticates the connection, enforcing the per-session failure lockout.
     *
     * @param parts LOGIN|customerId|pin
     * @return OK|customerName|accountNumbers, or an error response
     */
    private String login(String[] parts) {
        if (parts.length < 3) return "ERROR|Usage: LOGIN|customerId|pin";

        if (session.isAuthenticated()) {
            return "ERROR|Already logged in. Disconnect to switch customers.";
        }

        // SECURITY: temporary lockout after repeated failures slows online guessing
        if (session.isLockedOut()) {
            return "ERROR|Too many failed attempts. Try again in "
                    + session.getLockoutRemainingSeconds() + " seconds.";
        }

        Customer customer = bankService.authenticate(parts[1], parts[2]);

        if (customer == null) {
            // One generic message for unknown user, inactive user AND wrong PIN:
            // an attacker must not be able to enumerate valid customer IDs
            if (session.recordFailedLogin()) {
                return "ERROR|Too many failed attempts. Locked for "
                        + session.getLockoutRemainingSeconds() + " seconds.";
            }
            return "ERROR|Invalid customer ID or PIN.";
        }

        session.authenticate(customer.getCustomerId(), customer.getName());
        return "OK|" + customer.getName() + "|" + String.join(",", customer.getAccountNumbers());
    }

    /**
     * Closes every resource this connection holds. Always runs.
     */
    private void cleanup() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[Handler] Error during cleanup: " + e.getMessage());
        }
    }
}
