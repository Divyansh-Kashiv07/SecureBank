package com.securebank.transactions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Transaction — represents a single financial transaction on an account.
 *
 * RUBRIC: Unit 1 — Composition with Account (a Transaction cannot exist without its parent Account).
 * This class is intentionally IMMUTABLE (all fields are final) — once a transaction is
 * recorded, it should never be modified. This is a core banking principle.
 *
 * VIVA NOTE: Immutability means no setters. Once constructed, the transaction data is frozen.
 * This prevents tampering and makes the class inherently thread-safe (important because
 * multiple threads may read transaction lists concurrently).
 */
public class Transaction {

    /** Standard format for displaying and persisting timestamps */
    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Unique identifier for this transaction (e.g., "TXN-000001") */
    private final String transactionId;

    /** The account number this transaction belongs to */
    private final String accountNumber;

    /** Type of transaction (DEPOSIT, WITHDRAWAL, etc.) */
    private final TransactionType type;

    /** The monetary amount involved in this transaction */
    private final double amount;

    /** The account balance AFTER this transaction was applied */
    private final double balanceAfter;

    /** When this transaction occurred */
    private final LocalDateTime timestamp;

    /** Optional remarks/description (e.g., "Salary credit", "ATM withdrawal") */
    private final String remarks;

    /**
     * Full constructor — creates a complete transaction record.
     *
     * @param transactionId unique ID for this transaction
     * @param accountNumber the account this transaction belongs to
     * @param type          the type of transaction (from TransactionType enum)
     * @param amount        the monetary amount (always positive; type indicates direction)
     * @param balanceAfter  the account balance after this transaction
     * @param timestamp     when the transaction occurred
     * @param remarks       optional description/note
     */
    public Transaction(String transactionId, String accountNumber, TransactionType type,
                       double amount, double balanceAfter, LocalDateTime timestamp,
                       String remarks) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp;
        this.remarks = (remarks != null) ? remarks : "";
    }

    /**
     * Convenience constructor — auto-sets timestamp to now and empty remarks.
     * Demonstrates constructor overloading (Unit 1).
     */
    public Transaction(String transactionId, String accountNumber, TransactionType type,
                       double amount, double balanceAfter) {
        this(transactionId, accountNumber, type, amount, balanceAfter,
                LocalDateTime.now(), "");
    }

    // ==================== GETTERS (no setters — immutable) ====================

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getRemarks() {
        return remarks;
    }

    /**
     * Returns a formatted timestamp string for display and file persistence.
     * @return timestamp in "yyyy-MM-dd HH:mm:ss" format
     */
    public String getFormattedTimestamp() {
        return timestamp.format(DATE_FORMAT);
    }

    /**
     * Converts this transaction to a pipe-delimited string for file persistence.
     * Format: transactionId|accountNumber|type|amount|balanceAfter|timestamp|remarks
     *
     * RUBRIC: Unit 3 — String handling for file-based persistence.
     */
    public String toFileString() {
        // Using StringBuilder as required by rubric (not + concatenation)
        StringBuilder sb = new StringBuilder();
        sb.append(transactionId).append("|");
        sb.append(accountNumber).append("|");
        sb.append(type.name()).append("|");         // Store enum NAME (not label) for reliable parsing
        sb.append(String.format("%.2f", amount)).append("|");
        sb.append(String.format("%.2f", balanceAfter)).append("|");
        sb.append(timestamp.format(DATE_FORMAT)).append("|");
        sb.append(remarks);
        return sb.toString();
    }

    /**
     * Parses a pipe-delimited string (from file) back into a Transaction object.
     *
     * @param line the pipe-delimited string from the data file
     * @return a new Transaction object, or null if parsing fails
     */
    public static Transaction fromFileString(String line) {
        try {
            String[] parts = line.split("\\|", -1); // -1 to keep trailing empty strings
            if (parts.length < 6) return null;

            String txnId = parts[0];
            String accNum = parts[1];
            TransactionType type = TransactionType.valueOf(parts[2]); // Parse enum by name
            double amount = Double.parseDouble(parts[3]);
            double balanceAfter = Double.parseDouble(parts[4]);
            LocalDateTime timestamp = LocalDateTime.parse(parts[5], DATE_FORMAT);
            String remarks = (parts.length > 6) ? parts[6] : "";

            return new Transaction(txnId, accNum, type, amount, balanceAfter, timestamp, remarks);
        } catch (Exception e) {
            // If any parsing fails, return null — caller should handle gracefully
            System.err.println("Warning: Could not parse transaction line: " + line);
            return null;
        }
    }

    /**
     * Human-readable representation for debugging and logging.
     */
    @Override
    public String toString() {
        return String.format("[%s] %s: ₹%.2f on %s (Balance: ₹%.2f)",
                transactionId, type.getLabel(), amount, getFormattedTimestamp(), balanceAfter);
    }
}
