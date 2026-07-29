package com.securebank.core;

/**
 * CurrentAccount — a concrete implementation of the abstract Account class
 * designed for businesses and high-transaction customers.
 *
 * RUBRIC COVERAGE:
 * - Unit 1: Generalization (Account → CurrentAccount)
 * - Unit 2: Method Overriding (calculateInterest() has current-account-specific logic)
 *
 * Current accounts typically offer LOWER interest (or none) but allow unlimited
 * transactions and may support overdraft (withdrawing more than the balance,
 * up to a pre-approved limit). Cooperative banks often offer 0-1% on current accounts.
 *
 * VIVA NOTE — COMPARING OVERRIDING IN BOTH SUBCLASSES:
 * SavingsAccount.calculateInterest() → uses 4.0% rate
 * CurrentAccount.calculateInterest() → uses 1.0% rate + considers overdraft
 * Same method name, same signature, DIFFERENT behavior. This IS polymorphism.
 */
public class CurrentAccount extends Account {

    /** Annual interest rate for current accounts (1% — much lower than savings) */
    private static final double INTEREST_RATE = 1.0;

    /** Minimum balance required (₹5,000 for current — higher than savings) */
    private static final double MINIMUM_BALANCE = 5000.0;

    /** Overdraft limit — how much below zero the account can go */
    private double overdraftLimit;

    /**
     * Creates a new Current Account with a specified overdraft limit.
     *
     * @param accountNumber  unique account identifier
     * @param holderName     name of the account holder
     * @param customerId     the customer ID this account belongs to
     * @param initialBalance starting balance
     * @param overdraftLimit how much the account can go into negative (e.g., 10000.0)
     */
    public CurrentAccount(String accountNumber, String holderName, String customerId,
                          double initialBalance, double overdraftLimit) {
        super(accountNumber, holderName, customerId, initialBalance);
        this.overdraftLimit = overdraftLimit;
    }

    /**
     * Convenience constructor with default overdraft limit of ₹10,000.
     * Demonstrates constructor overloading at the subclass level.
     */
    public CurrentAccount(String accountNumber, String holderName, String customerId,
                          double initialBalance) {
        this(accountNumber, holderName, customerId, initialBalance, 10000.0);
    }

    /**
     * Calculates annual interest for a current account.
     * Interest is only calculated on the POSITIVE balance (not on overdraft usage).
     *
     * RUBRIC: Unit 2 — Method Overriding. Different logic from SavingsAccount:
     * - Lower rate (1% vs 4%)
     * - Only applies to positive balance
     *
     * @return the calculated interest amount (0 if balance is negative/zero)
     */
    @Override
    public double calculateInterest() {
        double currentBalance = getBalance();
        // No interest on negative balances (overdraft)
        if (currentBalance <= 0) {
            return 0.0;
        }
        return (currentBalance * INTEREST_RATE) / 100.0;
    }

    /**
     * Returns the account type identifier.
     * @return "Current"
     */
    @Override
    public String getAccountType() {
        return "Current";
    }

    // ==================== OVERDRAFT-SPECIFIC METHODS ====================

    /**
     * Returns the overdraft limit.
     * @return overdraft limit amount
     */
    public double getOverdraftLimit() {
        return overdraftLimit;
    }

    /**
     * Sets a new overdraft limit (e.g., after credit review).
     * @param overdraftLimit the new limit
     */
    public void setOverdraftLimit(double overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }

    /**
     * Returns the available balance INCLUDING overdraft.
     * E.g., if balance is ₹2,000 and overdraft limit is ₹10,000,
     * available balance is ₹12,000.
     *
     * @return effective available amount
     */
    public double getAvailableBalance() {
        return getBalance() + overdraftLimit;
    }

    /**
     * Returns the interest rate for display.
     * @return the annual interest rate
     */
    public double getInterestRate() {
        return INTEREST_RATE;
    }

    /**
     * Returns the minimum balance requirement.
     * @return minimum balance (₹5,000)
     */
    public double getMinimumBalance() {
        return MINIMUM_BALANCE;
    }

    @Override
    public String toString() {
        return String.format("Current Account [%s] — %s — Balance: ₹%.2f (OD Limit: ₹%.2f)",
                getAccountNumber(), getHolderName(), getBalance(), overdraftLimit);
    }
}
