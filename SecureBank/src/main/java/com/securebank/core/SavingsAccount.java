package com.securebank.core;

/**
 * SavingsAccount — a concrete implementation of the abstract Account class.
 *
 * RUBRIC COVERAGE:
 * - Unit 1: Generalization (Account → SavingsAccount)
 * - Unit 2: Method Overriding (calculateInterest() has savings-specific logic)
 *
 * Savings accounts earn interest on their balance. In India, cooperative banks
 * typically offer 4% p.a. on savings accounts. Interest is calculated on the
 * current balance using simple interest formula: (Balance × Rate × 1 year) / 100.
 *
 * VIVA NOTE — METHOD OVERRIDING:
 * The parent class Account declares calculateInterest() as abstract — it has NO body.
 * SavingsAccount MUST provide an implementation (the compiler enforces this).
 * This is runtime polymorphism: if you have an Account reference pointing to a
 * SavingsAccount object, calling calculateInterest() runs the SavingsAccount version.
 *
 *   Account acc = new SavingsAccount(...);  // Upcasting
 *   acc.calculateInterest();               // Runs SavingsAccount's version!
 */
public class SavingsAccount extends Account {

    /** Annual interest rate for savings accounts (4% is typical for cooperative banks) */
    private static final double INTEREST_RATE = 4.0;

    /** Minimum balance required (₹1,000 for savings) */
    private static final double MINIMUM_BALANCE = 1000.0;

    /**
     * Creates a new Savings Account.
     *
     * @param accountNumber unique account identifier
     * @param holderName    name of the account holder
     * @param customerId    the customer ID this account belongs to
     * @param initialBalance starting balance
     */
    public SavingsAccount(String accountNumber, String holderName, String customerId,
                          double initialBalance) {
        // Call the parent (Account) constructor using super()
        super(accountNumber, holderName, customerId, initialBalance);
    }

    /**
     * Calculates annual interest for a savings account.
     * Formula: (Balance × Rate) / 100
     *
     * RUBRIC: Unit 2 — Method Overriding. This implementation is DIFFERENT from
     * CurrentAccount's calculateInterest(). The JVM decides which to call at runtime
     * based on the actual object type, not the reference type.
     *
     * @return the calculated interest amount
     */
    @Override
    public double calculateInterest() {
        return (getBalance() * INTEREST_RATE) / 100.0;
    }

    /**
     * Returns the account type identifier.
     * @return "Savings"
     */
    @Override
    public String getAccountType() {
        return "Savings";
    }

    /**
     * Returns the interest rate for display purposes.
     * @return the annual interest rate (e.g., 4.0)
     */
    public double getInterestRate() {
        return INTEREST_RATE;
    }

    /**
     * Returns the minimum balance requirement.
     * @return minimum balance (₹1,000)
     */
    public double getMinimumBalance() {
        return MINIMUM_BALANCE;
    }

    @Override
    public String toString() {
        return String.format("Savings Account [%s] — %s — Balance: ₹%.2f (Rate: %.1f%%)",
                getAccountNumber(), getHolderName(), getBalance(), INTEREST_RATE);
    }
}
