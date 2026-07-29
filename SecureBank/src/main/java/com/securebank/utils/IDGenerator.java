package com.securebank.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * IDGenerator — generates unique IDs for accounts, customers, and transactions.
 *
 * Uses AtomicInteger for thread-safe ID generation without synchronized blocks.
 * AtomicInteger uses CPU-level compare-and-swap (CAS) operations which are
 * faster than synchronized for simple counter increments.
 *
 * VIVA NOTE: AtomicInteger is part of java.util.concurrent.atomic package.
 * It's thread-safe without using synchronized — instead it uses low-level
 * hardware instructions (CAS) to atomically increment the value.
 */
public class IDGenerator {

    /** Counter for account numbers — starts at 1000 for realistic-looking IDs */
    private static final AtomicInteger accountCounter = new AtomicInteger(1000);

    /** Counter for customer IDs */
    private static final AtomicInteger customerCounter = new AtomicInteger(0);

    /** Counter for transaction IDs */
    private static final AtomicInteger transactionCounter = new AtomicInteger(0);

    /** Counter for loan IDs */
    private static final AtomicInteger loanCounter = new AtomicInteger(0);

    /**
     * Generates a unique account number.
     * Format: "ACC-001001", "ACC-001002", etc.
     *
     * @return a unique account number string
     */
    public static String generateAccountNumber() {
        int id = accountCounter.incrementAndGet();
        return String.format("ACC-%06d", id);
    }

    /**
     * Generates a unique customer ID.
     * Format: "CUSTOMER-1", "CUSTOMER-2", etc.
     *
     * @return a unique customer ID string
     */
    public static String generateCustomerId() {
        int id = customerCounter.incrementAndGet();
        return String.format("CUSTOMER-%d", id);
    }

    /**
     * Generates a unique transaction ID.
     * Format: "TXN-000001", "TXN-000002", etc.
     *
     * @return a unique transaction ID string
     */
    public static String generateTransactionId() {
        int id = transactionCounter.incrementAndGet();
        return String.format("TXN-%06d", id);
    }

    /**
     * Generates a unique loan ID.
     * Format: "LOAN-000001", "LOAN-000002", etc.
     *
     * @return a unique loan ID string
     */
    public static String generateLoanId() {
        int id = loanCounter.incrementAndGet();
        return String.format("LOAN-%06d", id);
    }

    /**
     * Sets the counters to resume from saved state (called during file loading).
     * This prevents ID collisions after a server restart.
     *
     * @param maxAccount     the highest existing account number suffix
     * @param maxCustomer    the highest existing customer ID suffix
     * @param maxTransaction the highest existing transaction ID suffix
     * @param maxLoan        the highest existing loan ID suffix
     */
    public static void initializeCounters(int maxAccount, int maxCustomer,
                                           int maxTransaction, int maxLoan) {
        accountCounter.set(Math.max(1000, maxAccount));
        customerCounter.set(Math.max(0, maxCustomer));
        transactionCounter.set(maxTransaction);
        loanCounter.set(maxLoan);
    }

    /**
     * Extracts the numeric suffix from an ID string.
     * E.g., "ACC-001005" → 1005, "TXN-000042" → 42
     *
     * @param id the ID string to parse
     * @return the numeric suffix, or 0 if parsing fails
     */
    public static int extractNumber(String id) {
        try {
            // Split on "-" and parse the number part
            String[] parts = id.split("-");
            if (parts.length == 2) {
                return Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException e) {
            System.err.println("Warning: Could not extract number from ID: " + id);
        }
        return 0;
    }
}
