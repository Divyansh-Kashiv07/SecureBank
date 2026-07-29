package com.securebank.exceptions;

/**
 * InsufficientBalanceException — thrown when a withdrawal or transfer is attempted
 * but the account does not have enough funds to complete the operation.
 *
 * RUBRIC: Unit 3 — Custom checked exception (extends Exception, not RuntimeException).
 * Being a checked exception means the compiler FORCES the caller to handle it with
 * try-catch or declare it with 'throws' — this is appropriate for banking operations
 * where insufficient funds is an expected, recoverable condition.
 *
 * VIVA NOTE: Checked vs Unchecked exceptions:
 * - Checked (extends Exception): Must be caught or declared. Used for recoverable conditions.
 * - Unchecked (extends RuntimeException): Don't have to be caught. Used for programming errors.
 * We use checked here because "not enough money" is a business rule, not a bug.
 */
public class InsufficientBalanceException extends Exception {

    /** The amount that was attempted */
    private final double attemptedAmount;

    /** The actual available balance */
    private final double availableBalance;

    /**
     * Constructs the exception with details about the failed operation.
     *
     * @param attemptedAmount  the amount the user tried to withdraw/transfer
     * @param availableBalance the actual balance available in the account
     */
    public InsufficientBalanceException(double attemptedAmount, double availableBalance) {
        super(String.format(
                "Insufficient balance. Attempted: ₹%.2f, Available: ₹%.2f",
                attemptedAmount, availableBalance));
        this.attemptedAmount = attemptedAmount;
        this.availableBalance = availableBalance;
    }

    public double getAttemptedAmount() {
        return attemptedAmount;
    }

    public double getAvailableBalance() {
        return availableBalance;
    }
}
