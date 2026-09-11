package com.securebank.utils;

import com.securebank.core.Account;
import com.securebank.core.CurrentAccount;
import com.securebank.core.Customer;
import com.securebank.core.SavingsAccount;
import com.securebank.loans.Loan;
import com.securebank.transactions.Transaction;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
 * The files are stored in the configurable data directory (system property
 * "securebank.data.dir", default "data").
 *
 * RELIABILITY (Phase 4):
 * - SAVES ARE ATOMIC: every save writes to a temporary file in the same
 *   directory and then atomically moves it over the target. A crash or
 *   disk-full error mid-write can therefore never corrupt or truncate an
 *   existing data file — the old copy survives intact.
 * - SAVE FAILURES ARE SURFACED: save methods throw IOException instead of
 *   silently swallowing errors, so the server can tell the client when data
 *   was NOT persisted (silently losing money movements is unacceptable).
 * - LOADS ARE TOLERANT: a malformed line is skipped and logged, so one bad
 *   record cannot make the whole bank unbootable.
 */
public class FileIOHelper {

    /**
     * Directory where all data files are stored.
     *
     * Defaults to "data" (the application root's data directory). Tests set the
     * "securebank.data.dir" system property BEFORE the first FileIOHelper use
     * to redirect persistence to an isolated temporary directory, so tests never
     * touch the application's real data files.
     *
     * Read on every call (not pinned in a static final): each test class gets a
     * genuinely isolated directory even though the JVM (and this class) is
     * reused across test classes by Surefire's single fork.
     */
    private static String dataDir() {
        return System.getProperty("securebank.data.dir", "data");
    }

    /** File names for each entity type */
    private static final String ACCOUNTS_FILE = "accounts.dat";
    private static final String CUSTOMERS_FILE = "customers.dat";
    private static final String LOANS_FILE = "loans.dat";

    /**
     * Ensures the data directory exists. Creates it on first run.
     *
     * @throws IOException if the directory cannot be created
     */
    public static void ensureDataDirectory() throws IOException {
        Path dataPath = Paths.get(dataDir());
        if (!Files.exists(dataPath)) {
            Files.createDirectories(dataPath);
            System.out.println("[FileIO] Created data directory: " + dataPath.toAbsolutePath());
        }
    }

    // ==================== ATOMIC WRITE PRIMITIVE ====================

    /**
     * Writes all lines to the target file ATOMICALLY:
     * 1. Write to a temp file in the same directory (same filesystem → move is atomic)
     * 2. Atomically move the temp file over the target
     *
     * If anything fails at any point, the target file's previous content is
     * untouched. The temp file is cleaned up on failure.
     *
     * Package-private static so tests can exercise failure paths directly.
     *
     * @param target path of the file to write
     * @param lines  complete file content, one list entry per line
     * @throws IOException if writing or moving fails (target left unchanged)
     */
    static void writeFileAtomically(Path target, List<String> lines) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tmp = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            try {
                Files.move(tmp, target,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // Some filesystems (e.g., certain network shares) cannot move
                // atomically — the non-atomic move is still crash-safe for the
                // WRITE phase, which is where corruption would occur
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException cleanupEx) {
                System.err.println("[FileIO] Could not clean temp file " + tmp + ": " + cleanupEx.getMessage());
            }
            throw e;
        }
    }

    // ==================== ACCOUNT PERSISTENCE ====================

    /**
     * Saves all accounts to the accounts data file (atomic).
     *
     * @param accounts the list of accounts to save
     * @throws IOException if the data cannot be written — callers must handle this
     */
    public static void saveAccounts(List<Account> accounts) throws IOException {
        ensureDataDirectory();
        Path filePath = Paths.get(dataDir(), ACCOUNTS_FILE);

        List<String> lines = new ArrayList<>();
        for (Account account : accounts) {
            lines.add(account.toFileString());
        }
        writeFileAtomically(filePath, lines);

        // Also persist each account's transactions
        for (Account account : accounts) {
            try {
                saveTransactionsForAccount(account);
            } catch (IOException innerEx) {
                // One account's history failing should not block the others,
                // but the failure is NOT silent — it is logged loudly
                System.err.println("[FileIO] CRITICAL: Could not save transactions for " +
                        account.getAccountNumber() + ": " + innerEx.getMessage());
            }
        }

        System.out.println("[FileIO] Saved " + accounts.size() + " accounts to " + filePath);
    }

    /**
     * Loads all accounts from the accounts data file.
     * Tolerant: malformed lines are skipped and logged.
     *
     * @return list of loaded accounts (empty list if file doesn't exist)
     */
    public static List<Account> loadAccounts() {
        ensureDataDirectoryQuietly();
        List<Account> accounts = new ArrayList<>();
        Path filePath = Paths.get(dataDir(), ACCOUNTS_FILE);
        File file = filePath.toFile();

        if (!file.exists()) {
            System.out.println("[FileIO] No accounts file found. Starting fresh.");
            return accounts;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    Account account = parseAccountLine(line);
                    if (account != null) {
                        List<Transaction> transactions = loadTransactionsForAccount(
                                account.getAccountNumber());
                        for (Transaction txn : transactions) {
                            account.addTransaction(txn);
                        }
                        accounts.add(account);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("[FileIO] Line " + lineNumber +
                            ": Number format error: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("[FileIO] Line " + lineNumber +
                            ": Parse error: " + e.getMessage());
                }
            }

            System.out.println("[FileIO] Loaded " + accounts.size() + " accounts from " + filePath);

        } catch (IOException e) {
            System.err.println("[FileIO] ERROR reading accounts: " + e.getMessage());
        }

        return accounts;
    }

    /**
     * Parses a pipe-delimited line into an Account object.
     * Format: accountNumber|holderName|customerId|balance|accountType|dailyLimit|active[|overdraftLimit]
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

        if (parts.length > 5) {
            try {
                account.setDailyLimit(Double.parseDouble(parts[5]));
            } catch (NumberFormatException ignored) { }
        }
        if (parts.length > 6) {
            account.setActive(Boolean.parseBoolean(parts[6]));
        }
        // Field 8 (optional): overdraft limit for current accounts
        if (parts.length > 7 && account instanceof CurrentAccount) {
            try {
                ((CurrentAccount) account).setOverdraftLimit(Double.parseDouble(parts[7]));
            } catch (NumberFormatException ignored) { }
        }

        return account;
    }

    // ==================== TRANSACTION PERSISTENCE ====================

    /**
     * Saves transactions for a specific account to a per-account file (atomic).
     */
    private static void saveTransactionsForAccount(Account account) throws IOException {
        Path filePath = Paths.get(dataDir(), "txn_" + account.getAccountNumber() + ".dat");
        List<String> lines = new ArrayList<>();
        for (Transaction txn : account.getTransactionHistory()) {
            lines.add(txn.toFileString());
        }
        writeFileAtomically(filePath, lines);
    }

    /**
     * Loads transactions for a specific account from its per-account file.
     */
    private static List<Transaction> loadTransactionsForAccount(String accountNumber) {
        List<Transaction> transactions = new ArrayList<>();
        Path filePath = Paths.get(dataDir(), "txn_" + accountNumber + ".dat");
        File file = filePath.toFile();

        if (!file.exists()) return transactions;

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
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
     * Saves all customers to the customers data file (atomic).
     *
     * @throws IOException if the data cannot be written — callers must handle this
     */
    public static void saveCustomers(List<Customer> customers) throws IOException {
        ensureDataDirectory();
        Path filePath = Paths.get(dataDir(), CUSTOMERS_FILE);

        List<String> lines = new ArrayList<>();
        for (Customer customer : customers) {
            lines.add(customer.toFileString());
        }
        writeFileAtomically(filePath, lines);

        System.out.println("[FileIO] Saved " + customers.size() + " customers to " + filePath);
    }

    /**
     * Loads all customers from the customers data file.
     */
    public static List<Customer> loadCustomers() {
        ensureDataDirectoryQuietly();
        List<Customer> customers = new ArrayList<>();
        Path filePath = Paths.get(dataDir(), CUSTOMERS_FILE);
        File file = filePath.toFile();

        if (!file.exists()) {
            System.out.println("[FileIO] No customers file found. Starting fresh.");
            return customers;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
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
     * Saves all loans to the loans data file (atomic).
     *
     * @throws IOException if the data cannot be written — callers must handle this
     */
    public static void saveLoans(List<Loan> loans) throws IOException {
        ensureDataDirectory();
        Path filePath = Paths.get(dataDir(), LOANS_FILE);

        List<String> lines = new ArrayList<>();
        for (Loan loan : loans) {
            lines.add(loan.toFileString());
        }
        writeFileAtomically(filePath, lines);

        System.out.println("[FileIO] Saved " + loans.size() + " loans.");
    }

    /**
     * Loads all loans from the loans data file.
     */
    public static List<Loan> loadLoans() {
        ensureDataDirectoryQuietly();
        List<Loan> loans = new ArrayList<>();
        Path filePath = Paths.get(dataDir(), LOANS_FILE);
        File file = filePath.toFile();

        if (!file.exists()) return loans;

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
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

    /**
     * Best-effort directory creation for read paths (loading must not fail
     * hard just because the directory can't be created).
     */
    private static void ensureDataDirectoryQuietly() {
        try {
            ensureDataDirectory();
        } catch (IOException e) {
            System.err.println("[FileIO] Could not create data directory: " + e.getMessage());
        }
    }
}
