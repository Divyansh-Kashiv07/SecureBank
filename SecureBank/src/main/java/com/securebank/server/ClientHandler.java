package com.securebank.server;

import com.securebank.core.*;
import com.securebank.exceptions.*;
import com.securebank.loans.Loan;
import com.securebank.loans.LoanProcessor;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.CustomerRepository;
import com.securebank.transactions.Transaction;
import com.securebank.transactions.TransactionLogger;
import com.securebank.utils.IDGenerator;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ClientHandler — handles communication with a single connected client.
 *
 * RUBRIC COVERAGE:
 * - Unit 4: implements Runnable — each instance runs on its own thread.
 * - Unit 4: Socket I/O — reads requests from and writes responses to the client.
 * - Unit 3: Exception handling — try-catch, multiple catch, nested try, finally, throw/throws.
 *
 * PROTOCOL:
 * Request format:  COMMAND|param1|param2|...
 * Response format: OK|data   or   ERROR|message
 *
 * Supported commands:
 *   LOGIN|customerId|pin           → OK|customerName|accountNumbers
 *   BALANCE|accountNumber          → OK|balance
 *   DEPOSIT|accountNumber|amount|remarks → OK|newBalance|transactionId
 *   WITHDRAW|accountNumber|amount  → OK|newBalance|transactionId
 *   TRANSFER|fromAcc|toAcc|amount  → OK|newBalance|transactionId
 *   HISTORY|accountNumber          → OK|txn1;txn2;txn3...
 *   ACCOUNT_INFO|accountNumber     → OK|holderName|type|balance|interestRate
 *   LOAN_APPLY|custId|accNum|amount|tenure|purpose → OK|loanId|emi
 *   LOAN_STATUS|customerId         → OK|loan1;loan2;...
 *   ACCOUNTS|customerId            → OK|acc1;acc2;...
 *   CREATE_ACCOUNT|custId|type|initBalance → OK|accountNumber
 *   INTEREST|accountNumber         → OK|interestAmount
 *   SAVE                           → OK|saved
 *
 * VIVA NOTE — RUNNABLE vs THREAD:
 * We implement Runnable (not extend Thread) because:
 * 1. Java supports single inheritance — if we extend Thread, we can't extend anything else.
 * 2. Runnable separates the TASK (what to do) from the MECHANISM (how to run it).
 * 3. The same Runnable can be submitted to a Thread, ExecutorService, or thread pool.
 * 4. Better design — "prefer composition over inheritance."
 */
public class ClientHandler implements Runnable {

    /** The socket connection to this specific client */
    private final Socket clientSocket;

    /** Shared account repository — access MUST be synchronized */
    private final AccountRepository accountRepository;

    /** Shared customer repository */
    private final CustomerRepository customerRepository;

    /** Loan processor for loan operations */
    private final LoanProcessor loanProcessor;

    /** Transaction logger daemon for async logging */
    private final TransactionLogger transactionLogger;

    /** Reader for incoming client messages */
    private BufferedReader in;

    /** Writer for outgoing server responses */
    private PrintWriter out;

    /**
     * Creates a new ClientHandler for the given client socket.
     *
     * @param clientSocket      the client's socket connection
     * @param accountRepository shared account repository
     * @param customerRepository shared customer repository
     * @param loanProcessor     shared loan processor
     * @param transactionLogger shared daemon logger
     */
    public ClientHandler(Socket clientSocket, AccountRepository accountRepository,
                         CustomerRepository customerRepository, LoanProcessor loanProcessor,
                         TransactionLogger transactionLogger) {
        this.clientSocket = clientSocket;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.loanProcessor = loanProcessor;
        this.transactionLogger = transactionLogger;
    }

    /**
     * Main method executed when this handler's thread starts.
     *
     * RUBRIC: Unit 4 — Runnable.run() implementation.
     * This method:
     * 1. Sets up I/O streams on the socket
     * 2. Reads client requests in a loop
     * 3. Parses and dispatches each command
     * 4. Sends responses back
     * 5. Cleans up on disconnect
     */
    @Override
    public void run() {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        System.out.println("[Handler] Thread started for client: " + clientAddress);

        try {
            // Set up I/O streams on the socket
            // RUBRIC: Unit 4 — Character streams over socket for text-based protocol
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
            // 'true' enables auto-flush — every println() immediately sends data

            // Read and process commands until client disconnects
            String requestLine;
            while ((requestLine = in.readLine()) != null) {
                requestLine = requestLine.trim();
                if (requestLine.isEmpty()) continue;

                System.out.println("[Handler " + clientAddress + "] Request: " + requestLine);

                // Process the command and get the response
                String response = processCommand(requestLine);

                // Send response back to client
                out.println(response);
                System.out.println("[Handler " + clientAddress + "] Response: " + response);
            }

        } catch (IOException e) {
            System.out.println("[Handler " + clientAddress + "] Client disconnected: " + e.getMessage());
        } finally {
            // RUBRIC: Unit 3 — finally block for resource cleanup
            // This ALWAYS runs, even if an exception occurred.
            // We must close the socket to free the port and resources.
            cleanup();
            System.out.println("[Handler " + clientAddress + "] Thread finished.");
        }
    }

    /**
     * Parses a command string and dispatches to the appropriate handler method.
     *
     * @param request the raw request string (e.g., "DEPOSIT|ACC-001001|5000|Salary")
     * @return the response string (e.g., "OK|30000.00|TXN-000001")
     */
    private String processCommand(String request) {
        String[] parts = request.split("\\|", -1);
        String command = parts[0].toUpperCase();

        // RUBRIC: Unit 1 — Control statements (switch)
        try {
            switch (command) {
                case "LOGIN":
                    return handleLogin(parts);
                case "BALANCE":
                    return handleBalance(parts);
                case "DEPOSIT":
                    return handleDeposit(parts);
                case "WITHDRAW":
                    return handleWithdraw(parts);
                case "TRANSFER":
                    return handleTransfer(parts);
                case "HISTORY":
                    return handleHistory(parts);
                case "ACCOUNT_INFO":
                    return handleAccountInfo(parts);
                case "LOAN_APPLY":
                    return handleLoanApply(parts);
                case "LOAN_STATUS":
                    return handleLoanStatus(parts);
                case "ACCOUNTS":
                    return handleGetAccounts(parts);
                case "CREATE_ACCOUNT":
                    return handleCreateAccount(parts);
                case "INTEREST":
                    return handleInterest(parts);
                case "SAVE":
                    return handleSave();
                default:
                    return "ERROR|Unknown command: " + command;
            }
        } catch (Exception e) {
            // Catch-all for any unexpected errors
            return "ERROR|Internal server error: " + e.getMessage();
        }
    }

    // ==================== COMMAND HANDLERS ====================

    /**
     * Handles LOGIN|customerId|pin
     * Validates customer credentials.
     *
     * RUBRIC: Unit 3 — throws custom InvalidPinException, AccountNotFoundException.
     */
    private String handleLogin(String[] parts) {
        if (parts.length < 3) return "ERROR|Usage: LOGIN|customerId|pin";

        String customerId = parts[1];
        String pin = parts[2];

        // RUBRIC: nested try-catch with custom exceptions
        try {
            Customer customer = customerRepository.getCustomer(customerId);

            if (customer == null) {
                // RUBRIC: throw custom exception
                throw new AccountNotFoundException(customerId);
            }

            if (!customer.validatePin(pin)) {
                // RUBRIC: throw custom exception
                throw new InvalidPinException(customerId);
            }

            if (!customer.isActive()) {
                return "ERROR|Account is deactivated. Contact the bank.";
            }

            // Login successful — return customer name and their account numbers
            String accountNumbers = String.join(",", customer.getAccountNumbers());
            return "OK|" + customer.getName() + "|" + accountNumbers;

        } catch (AccountNotFoundException e) {
            return "ERROR|" + e.getMessage();
        } catch (InvalidPinException e) {
            return "ERROR|Invalid PIN. Please try again.";
        }
    }

    /**
     * Handles BALANCE|accountNumber
     */
    private String handleBalance(String[] parts) {
        if (parts.length < 2) return "ERROR|Usage: BALANCE|accountNumber";

        Account account = accountRepository.getAccount(parts[1]);
        if (account == null) {
            return "ERROR|Account not found: " + parts[1];
        }

        return "OK|" + String.format("%.2f", account.getBalance());
    }

    /**
     * Handles DEPOSIT|accountNumber|amount|remarks
     *
     * The deposit() method on Account is SYNCHRONIZED — only one thread can
     * execute it at a time for the same Account object. This prevents the
     * lost-update race condition described in Account.java.
     */
    private String handleDeposit(String[] parts) {
        if (parts.length < 3) return "ERROR|Usage: DEPOSIT|accountNumber|amount[|remarks]";

        String accountNumber = parts[1];
        double amount;

        try {
            amount = Double.parseDouble(parts[2]);
        } catch (NumberFormatException e) {
            return "ERROR|Invalid amount format";
        }

        Account account = accountRepository.getAccount(accountNumber);
        if (account == null) {
            return "ERROR|Account not found: " + accountNumber;
        }

        // This call is SYNCHRONIZED inside Account.deposit()
        String remarks = (parts.length > 3) ? parts[3] : "";
        double newBalance;

        if (remarks.isEmpty()) {
            newBalance = account.deposit(amount);           // Overloaded version 1
        } else {
            newBalance = account.deposit(amount, remarks);  // Overloaded version 2
        }

        // Log the transaction asynchronously via the daemon thread
        List<Transaction> history = account.getTransactionHistory();
        if (!history.isEmpty()) {
            Transaction lastTxn = history.get(history.size() - 1);
            transactionLogger.log(lastTxn);

            // Auto-save after each transaction
            accountRepository.saveToFile();

            return "OK|" + String.format("%.2f", newBalance) + "|" + lastTxn.getTransactionId();
        }

        return "OK|" + String.format("%.2f", newBalance) + "|N/A";
    }

    /**
     * Handles WITHDRAW|accountNumber|amount
     *
     * RUBRIC: Unit 3 — demonstrates try-catch with multiple custom exceptions.
     */
    private String handleWithdraw(String[] parts) {
        if (parts.length < 3) return "ERROR|Usage: WITHDRAW|accountNumber|amount";

        String accountNumber = parts[1];
        double amount;

        try {
            amount = Double.parseDouble(parts[2]);
        } catch (NumberFormatException e) {
            return "ERROR|Invalid amount format";
        }

        Account account = accountRepository.getAccount(accountNumber);
        if (account == null) {
            return "ERROR|Account not found: " + accountNumber;
        }

        // RUBRIC: try-catch with MULTIPLE catch blocks for different custom exceptions
        try {
            // This call is SYNCHRONIZED inside Account.withdraw()
            double newBalance = account.withdraw(amount);

            // Log asynchronously
            List<Transaction> history = account.getTransactionHistory();
            Transaction lastTxn = history.get(history.size() - 1);
            transactionLogger.log(lastTxn);

            // Auto-save
            accountRepository.saveToFile();

            return "OK|" + String.format("%.2f", newBalance) + "|" + lastTxn.getTransactionId();

        } catch (InsufficientBalanceException e) {
            // RUBRIC: Catch block 1 — insufficient balance
            return "ERROR|" + e.getMessage();
        } catch (DailyLimitExceededException e) {
            // RUBRIC: Catch block 2 — daily limit exceeded
            return "ERROR|" + e.getMessage();
        }
    }

    /**
     * Handles TRANSFER|fromAccount|toAccount|amount
     */
    private String handleTransfer(String[] parts) {
        if (parts.length < 4) return "ERROR|Usage: TRANSFER|fromAccount|toAccount|amount";

        String fromAcc = parts[1];
        String toAcc = parts[2];
        double amount;

        try {
            amount = Double.parseDouble(parts[3]);
        } catch (NumberFormatException e) {
            return "ERROR|Invalid amount format";
        }

        Account source = accountRepository.getAccount(fromAcc);
        Account target = accountRepository.getAccount(toAcc);

        if (source == null) return "ERROR|Source account not found: " + fromAcc;
        if (target == null) return "ERROR|Target account not found: " + toAcc;

        try {
            // transferTo() handles its own synchronization with lock ordering
            source.transferTo(target, amount);

            // Log both sides
            List<Transaction> sourceHistory = source.getTransactionHistory();
            List<Transaction> targetHistory = target.getTransactionHistory();
            if (!sourceHistory.isEmpty()) {
                transactionLogger.log(sourceHistory.get(sourceHistory.size() - 1));
            }
            if (!targetHistory.isEmpty()) {
                transactionLogger.log(targetHistory.get(targetHistory.size() - 1));
            }

            // Auto-save
            accountRepository.saveToFile();

            return "OK|" + String.format("%.2f", source.getBalance()) + "|" +
                    (sourceHistory.isEmpty() ? "N/A" :
                            sourceHistory.get(sourceHistory.size() - 1).getTransactionId());

        } catch (InsufficientBalanceException e) {
            return "ERROR|" + e.getMessage();
        } catch (DailyLimitExceededException e) {
            return "ERROR|" + e.getMessage();
        }
    }

    /**
     * Handles HISTORY|accountNumber
     * Returns transaction history as semicolon-separated entries.
     */
    private String handleHistory(String[] parts) {
        if (parts.length < 2) return "ERROR|Usage: HISTORY|accountNumber";

        Account account = accountRepository.getAccount(parts[1]);
        if (account == null) {
            return "ERROR|Account not found: " + parts[1];
        }

        List<Transaction> history = account.getTransactionHistory();
        if (history.isEmpty()) {
            return "OK|EMPTY";
        }

        // RUBRIC: Unit 2 — Lambda/Stream for mapping transactions to strings
        String historyStr = history.stream()
                .map(Transaction::toFileString)
                .collect(Collectors.joining(";"));

        return "OK|" + historyStr;
    }

    /**
     * Handles ACCOUNT_INFO|accountNumber
     */
    private String handleAccountInfo(String[] parts) {
        if (parts.length < 2) return "ERROR|Usage: ACCOUNT_INFO|accountNumber";

        Account account = accountRepository.getAccount(parts[1]);
        if (account == null) {
            return "ERROR|Account not found: " + parts[1];
        }

        StringBuilder info = new StringBuilder();
        info.append(account.getHolderName()).append("|");
        info.append(account.getAccountType()).append("|");
        info.append(String.format("%.2f", account.getBalance())).append("|");

        // Include interest rate based on account type
        if (account instanceof SavingsAccount) {
            info.append(((SavingsAccount) account).getInterestRate());
        } else if (account instanceof CurrentAccount) {
            info.append(((CurrentAccount) account).getInterestRate());
        } else {
            info.append("0.0");
        }

        return "OK|" + info.toString();
    }

    /**
     * Handles LOAN_APPLY|customerId|accountNumber|amount|tenureMonths|purpose
     */
    private String handleLoanApply(String[] parts) {
        if (parts.length < 6) return "ERROR|Usage: LOAN_APPLY|custId|accNum|amount|tenure|purpose";

        String customerId = parts[1];
        String accountNumber = parts[2];
        double amount;
        int tenure;

        try {
            amount = Double.parseDouble(parts[3]);
            tenure = Integer.parseInt(parts[4]);
        } catch (NumberFormatException e) {
            return "ERROR|Invalid amount or tenure format";
        }

        String purpose = parts[5];
        Account account = accountRepository.getAccount(accountNumber);

        Loan loan = loanProcessor.applyForLoan(customerId, accountNumber, amount,
                tenure, purpose, account);

        if (loan == null) {
            return "ERROR|Loan application rejected. Check eligibility requirements.";
        }

        // Auto-approve for demo purposes (in production, this would be manual review)
        loanProcessor.approveLoan(loan.getLoanId());
        if (account != null) {
            loanProcessor.disburseLoan(loan.getLoanId(), account);
            accountRepository.saveToFile();
        }
        loanProcessor.saveToFile();

        return "OK|" + loan.getLoanId() + "|" + String.format("%.2f", loan.getEmi());
    }

    /**
     * Handles LOAN_STATUS|customerId
     */
    private String handleLoanStatus(String[] parts) {
        if (parts.length < 2) return "ERROR|Usage: LOAN_STATUS|customerId";

        List<Loan> loans = loanProcessor.getLoansByCustomer(parts[1]);
        if (loans.isEmpty()) {
            return "OK|EMPTY";
        }

        String loansStr = loans.stream()
                .map(Loan::toFileString)
                .collect(Collectors.joining(";"));

        return "OK|" + loansStr;
    }

    /**
     * Handles ACCOUNTS|customerId — returns all accounts for a customer.
     */
    private String handleGetAccounts(String[] parts) {
        if (parts.length < 2) return "ERROR|Usage: ACCOUNTS|customerId";

        Customer customer = customerRepository.getCustomer(parts[1]);
        if (customer == null) {
            return "ERROR|Customer not found: " + parts[1];
        }

        List<String> accountNumbers = customer.getAccountNumbers();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < accountNumbers.size(); i++) {
            Account acc = accountRepository.getAccount(accountNumbers.get(i));
            if (acc != null) {
                if (i > 0) result.append(";");
                result.append(acc.getAccountNumber()).append(",");
                result.append(acc.getAccountType()).append(",");
                result.append(String.format("%.2f", acc.getBalance()));
            }
        }

        return result.length() > 0 ? "OK|" + result.toString() : "OK|EMPTY";
    }

    /**
     * Handles CREATE_ACCOUNT|customerId|type|initialBalance
     */
    private String handleCreateAccount(String[] parts) {
        if (parts.length < 4) return "ERROR|Usage: CREATE_ACCOUNT|custId|type|initBalance";

        String customerId = parts[1];
        String type = parts[2];
        double initialBalance;

        try {
            initialBalance = Double.parseDouble(parts[3]);
        } catch (NumberFormatException e) {
            return "ERROR|Invalid initial balance";
        }

        Customer customer = customerRepository.getCustomer(customerId);
        if (customer == null) {
            return "ERROR|Customer not found: " + customerId;
        }

        String accountNumber = IDGenerator.generateAccountNumber();
        Account account;

        if ("Savings".equalsIgnoreCase(type)) {
            account = new SavingsAccount(accountNumber, customer.getName(),
                    customerId, initialBalance);
        } else if ("Current".equalsIgnoreCase(type)) {
            account = new CurrentAccount(accountNumber, customer.getName(),
                    customerId, initialBalance);
        } else {
            return "ERROR|Invalid account type. Use 'Savings' or 'Current'.";
        }

        accountRepository.addAccount(account);
        customer.addAccount(accountNumber);

        // Save both repositories
        accountRepository.saveToFile();
        customerRepository.saveToFile();

        return "OK|" + accountNumber;
    }

    /**
     * Handles INTEREST|accountNumber — calculates and returns interest.
     */
    private String handleInterest(String[] parts) {
        if (parts.length < 2) return "ERROR|Usage: INTEREST|accountNumber";

        Account account = accountRepository.getAccount(parts[1]);
        if (account == null) {
            return "ERROR|Account not found: " + parts[1];
        }

        double interest = account.calculateInterest();
        return "OK|" + String.format("%.2f", interest);
    }

    /**
     * Handles SAVE — forces an immediate save of all data.
     */
    private String handleSave() {
        accountRepository.saveToFile();
        customerRepository.saveToFile();
        loanProcessor.saveToFile();
        return "OK|All data saved successfully";
    }

    /**
     * Cleans up resources when the client disconnects.
     * Called from the finally block in run().
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
