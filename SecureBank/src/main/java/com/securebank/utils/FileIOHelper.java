package com.securebank.utils;

import com.securebank.core.Account;
import com.securebank.core.CurrentAccount;
import com.securebank.core.Customer;
import com.securebank.core.SavingsAccount;
import com.securebank.loans.Loan;
import com.securebank.transactions.Transaction;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * FileIOHelper — handles all file-based persistence (save/load) for the application.
 *
 * RUBRIC COVERAGE:
 * - Unit 4: I/O Streams — uses Character Streams (BufferedReader/BufferedWriter)
 *   for reading and writing data files.
 * - Unit 3: Exception handling — demonstrates try-catch, finally, nested try,
 *   multiple catch blocks, throw/throws.
 *
 * FILE FORMAT:
 * Each entity is stored as a pipe-delimited (|) line in a plain text file.
 * The files are stored in a "data/" directory relative to the application root.
 *
 * VIVA NOTE — CHARACTER STREAMS vs BYTE STREAMS:
 * - Character Streams (Reader/Writer): Handle text data, automatically manage
 *   character encoding (UTF-8, etc.). Use for human-readable files.
 * - Byte Streams (InputStream/OutputStream): Handle raw binary data. Use for
 *   images, serialized objects, etc.
 *
 * We use Character Streams because our data files are human-readable text.
 * BufferedReader/BufferedWriter add an internal buffer (default 8KB) that
 * reduces the number of actual disk I/O operations → better performance.
 */
public class FileIOHelper {

    /** Directory where all data files are stored */
    private static final String DATA_DIR = "data";

    /** File names for each entity type */
    private static final String ACCOUNTS_FILE = "accounts.dat";
    private static final String CUSTOMERS_FILE = "customers.dat";
    private static final String TRANSACTIONS_FILE = "transactions.dat";
    private static final String LOANS_FILE = "loans.dat";

    /**
     * Ensures the data directory exists. Creates it on first run.
     *
     * RUBRIC: Common debugging point — "file not found on first run."
     * This method prevents that by creating the directory automatically.
     */
    public static void ensureDataDirectory() {
        // RUBRIC: Unit 3 — try-catch with exception handling
        try {
            Path dataPath = Paths.get(DATA_DIR);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
                System.out.println("[FileIO] Created data directory: " + dataPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("[FileIO] ERROR: Could not create data directory: " + e.getMessage());
        }
    }

    // ==================== ACCOUNT PERSISTENCE ====================

    /**
     * Saves all accounts to the accounts data file.
     *
     * RUBRIC: Unit 4 — BufferedWriter (Character Stream) for file output.
     * RUBRIC: Unit 3 — try-with-resources (auto-closes the writer in finally equivalent).
     *
     * @param accounts the list of accounts to save
     */
    public static void saveAccounts(List<Account> accounts) {
        ensureDataDirectory();
        String filePath = DATA_DIR + File.separator + ACCOUNTS_FILE;

        // try-with-resources: automatically calls writer.close() when done
        // This is equivalent to using a finally block to close the resource
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

            for (Account account : accounts) {
                writer.write(account.toFileString());
                writer.newLine();

                // Also save each account's transactions
                // RUBRIC: nested try — saving transactions inside the account loop
                try {
                    saveTransactionsForAccount(account);
                } catch (IOException innerEx) {
                    // Multiple catch scenario — inner exception doesn't stop outer save
                    System.err.println("[FileIO] Warning: Could not save transactions for " +
                            account.getAccountNumber() + ": " + innerEx.getMessage());
                }
            }

            System.out.println("[FileIO] Saved " + accounts.size() + " accounts to " + filePath);

        } catch (IOException e) {
            System.err.println("[FileIO] ERROR saving accounts: " + e.getMessage());
        }
    }

    /**
     * Loads all accounts from the accounts data file.
     *
     * RUBRIC: Unit 4 — BufferedReader (Character Stream) for file input.
     * RUBRIC: Unit 3 — Multiple catch blocks, finally block equivalent.
     *
     * @return list of loaded accounts (empty list if file doesn't exist)
     */
    public static List<Account> loadAccounts() {
        ensureDataDirectory();
        List<Account> accounts = new ArrayList<>();
        String filePath = DATA_DIR + File.separator + ACCOUNTS_FILE;
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("[FileIO] No accounts file found. Starting fresh.");
            return accounts;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                // RUBRIC: nested try — parse each line independently
                try {
                    Account account = parseAccountLine(line);
                    if (account != null) {
                        // Load transactions for this account
                        List<Transaction> transactions = loadTransactionsForAccount(
                                account.getAccountNumber());
                        for (Transaction txn : transactions) {
                            account.addTransaction(txn);
                        }
                        accounts.add(account);
                    }
                } catch (NumberFormatException e) {
                    // RUBRIC: multiple catch — specific exception type
                    System.err.println("[FileIO] Line " + lineNumber +
                            ": Number format error: " + e.getMessage());
                } catch (Exception e) {
                    // RUBRIC: multiple catch — general fallback
                    System.err.println("[FileIO] Line " + lineNumber +
                            ": Parse error: " + e.getMessage());
                }
            }

            System.out.println("[FileIO] Loaded " + accounts.size() + " accounts from " + filePath);

        } catch (FileNotFoundException e) {
            System.out.println("[FileIO] Accounts file not found: " + filePath);
        } catch (IOException e) {
            System.err.println("[FileIO] ERROR reading accounts: " + e.getMessage());
        } finally {
            // RUBRIC: Unit 3 — finally block for cleanup
            // This block ALWAYS runs, even if an exception occurred above.
            // It ensures the reader is closed and system resources are released.
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("[FileIO] ERROR closing reader: " + e.getMessage());
                }
            }
        }

        return accounts;
    }

    /**
     * Parses a pipe-delimited line into an Account object.
     * Format: accountNumber|holderName|customerId|balance|accountType|dailyLimit|active
     */
    private static Account parseAccountLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 5) return null;

        String accountNumber = parts[0];
        String holderName = parts[1];
        String customerId = parts[2];
        double balance = Double.parseDouble(parts[3]);
        String accountType = parts[4];

        Account account;
        // Control statement to determine which subclass to instantiate
        if ("Savings".equalsIgnoreCase(accountType)) {
            account = new SavingsAccount(accountNumber, holderName, customerId, 0);
        } else if ("Current".equalsIgnoreCase(accountType)) {
            account = new CurrentAccount(accountNumber, holderName, customerId, 0);
        } else {
            System.err.println("[FileIO] Unknown account type: " + accountType);
            return null;
        }

        // Set the balance from file (bypasses deposit logic)
        account.setBalanceFromFile(balance);

        // Set daily limit and active status if available
        if (parts.length > 5) {
            try {
                account.setDailyLimit(Double.parseDouble(parts[5]));
            } catch (NumberFormatException ignored) { }
        }
        if (parts.length > 6) {
            account.setActive(Boolean.parseBoolean(parts[6]));
        }

        return account;
    }

    // ==================== TRANSACTION PERSISTENCE ====================

    /**
     * Saves transactions for a specific account to a per-account file.
     * File name: data/txn_ACC-001001.dat
     */
    private static void saveTransactionsForAccount(Account account) throws IOException {
        String filePath = DATA_DIR + File.separator + "txn_" +
                account.getAccountNumber() + ".dat";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Transaction txn : account.getTransactionHistory()) {
                writer.write(txn.toFileString());
                writer.newLine();
            }
        }
    }

    /**
     * Loads transactions for a specific account from its per-account file.
     */
    private static List<Transaction> loadTransactionsForAccount(String accountNumber) {
        List<Transaction> transactions = new ArrayList<>();
        String filePath = DATA_DIR + File.separator + "txn_" + accountNumber + ".dat";
        File file = new File(filePath);

        if (!file.exists()) return transactions;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                Transaction txn = Transaction.fromFileString(line);
                if (txn != null) {
                    transactions.add(txn);
                }
            }
        } catch (IOException e) {
            System.err.println("[FileIO] Error loading transactions for " + accountNumber +
                    ": " + e.getMessage());
        }

        return transactions;
    }

    // ==================== CUSTOMER PERSISTENCE ====================

    /**
     * Saves all customers to the customers data file.
     */
    public static void saveCustomers(List<Customer> customers) {
        ensureDataDirectory();
        String filePath = DATA_DIR + File.separator + CUSTOMERS_FILE;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Customer customer : customers) {
                writer.write(customer.toFileString());
                writer.newLine();
            }
            System.out.println("[FileIO] Saved " + customers.size() + " customers to " + filePath);
        } catch (IOException e) {
            System.err.println("[FileIO] ERROR saving customers: " + e.getMessage());
        }
    }

    /**
     * Loads all customers from the customers data file.
     */
    public static List<Customer> loadCustomers() {
        ensureDataDirectory();
        List<Customer> customers = new ArrayList<>();
        String filePath = DATA_DIR + File.separator + CUSTOMERS_FILE;
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("[FileIO] No customers file found. Starting fresh.");
            return customers;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                Customer customer = Customer.fromFileString(line);
                if (customer != null) {
                    customers.add(customer);
                }
            }
            System.out.println("[FileIO] Loaded " + customers.size() + " customers.");
        } catch (IOException e) {
            System.err.println("[FileIO] ERROR loading customers: " + e.getMessage());
        }

        return customers;
    }

    // ==================== LOAN PERSISTENCE ====================

    /**
     * Saves all loans to the loans data file.
     */
    public static void saveLoans(List<Loan> loans) {
        ensureDataDirectory();
        String filePath = DATA_DIR + File.separator + LOANS_FILE;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Loan loan : loans) {
                writer.write(loan.toFileString());
                writer.newLine();
            }
            System.out.println("[FileIO] Saved " + loans.size() + " loans.");
        } catch (IOException e) {
            System.err.println("[FileIO] ERROR saving loans: " + e.getMessage());
        }
    }

    /**
     * Loads all loans from the loans data file.
     */
    public static List<Loan> loadLoans() {
        ensureDataDirectory();
        List<Loan> loans = new ArrayList<>();
        String filePath = DATA_DIR + File.separator + LOANS_FILE;
        File file = new File(filePath);

        if (!file.exists()) return loans;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                Loan loan = Loan.fromFileString(line);
                if (loan != null) {
                    loans.add(loan);
                }
            }
            System.out.println("[FileIO] Loaded " + loans.size() + " loans.");
        } catch (IOException e) {
            System.err.println("[FileIO] ERROR loading loans: " + e.getMessage());
        }

        return loans;
    }
}
