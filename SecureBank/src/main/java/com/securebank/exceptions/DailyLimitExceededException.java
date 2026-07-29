package com.securebank.exceptions;

/**
 * DailyLimitExceededException — thrown when a transaction would cause the
 * customer to exceed their daily withdrawal or transfer limit.
 *
 * RUBRIC: Unit 3 — Custom checked exception.
 * Banking systems enforce daily limits to prevent fraud and excessive withdrawals.
 * This exception provides details about the limit and the attempted total.
 */
public class DailyLimitExceededException extends Exception {

    /** The daily limit that was exceeded */
    private final double dailyLimit;

    /** The total amount already transacted today */
    private final double todayTotal;

    /** The amount that was attempted in this transaction */
    private final double attemptedAmount;

    /**
     * Constructs the exception with full details about the limit breach.
     *
     * @param dailyLimit      the maximum allowed daily amount
     * @param todayTotal      the total already transacted today
     * @param attemptedAmount the amount attempted in this transaction
     */
    public DailyLimitExceededException(double dailyLimit, double todayTotal,
                                        double attemptedAmount) {
        super(String.format(
                "Daily limit exceeded. Limit: ₹%.2f, Today's total: ₹%.2f, Attempted: ₹%.2f",
                dailyLimit, todayTotal, attemptedAmount));
        this.dailyLimit = dailyLimit;
        this.todayTotal = todayTotal;
        this.attemptedAmount = attemptedAmount;
    }

    public double getDailyLimit() {
        return dailyLimit;
    }

    public double getTodayTotal() {
        return todayTotal;
    }

    public double getAttemptedAmount() {
        return attemptedAmount;
    }
}
