package com.securebank.transactions;

/**
 * TransactionType Enum — categorizes every transaction in the system.
 *
 * RUBRIC: Enums are a clean way to represent fixed sets of constants.
 * Each type maps to a human-readable label for display in the GUI and receipts.
 *
 * VIVA NOTE: Enums in Java are more powerful than simple constants — they are
 * full classes that can have fields, constructors, and methods. Here we use
 * a 'label' field so we can display "Fund Transfer (In)" instead of "TRANSFER_IN".
 */
public enum TransactionType {

    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    TRANSFER_IN("Fund Transfer (In)"),
    TRANSFER_OUT("Fund Transfer (Out)"),
    INTEREST("Interest Credit"),
    LOAN_DISBURSEMENT("Loan Disbursement"),
    LOAN_REPAYMENT("Loan Repayment");

    /** Human-readable label for display in GUI and receipts */
    private final String label;

    /**
     * Constructor for enum constants.
     * @param label the display-friendly name of the transaction type
     */
    TransactionType(String label) {
        this.label = label;
    }

    /**
     * Returns the human-readable label.
     * @return display label (e.g., "Deposit", "Fund Transfer (In)")
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the label when converting to string (used in file persistence).
     */
    @Override
    public String toString() {
        return label;
    }
}
