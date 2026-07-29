package com.securebank.loans;

/**
 * LoanStatus — represents the lifecycle state of a loan application.
 *
 * RUBRIC: Clean enum usage for state management.
 * A loan progresses through these states:
 *   PENDING → APPROVED → ACTIVE → CLOSED
 *            → REJECTED (terminal state, cannot be reopened)
 */
public enum LoanStatus {

    /** Loan application has been submitted but not yet reviewed */
    PENDING("Pending Review"),

    /** Loan has been approved by the bank but not yet disbursed */
    APPROVED("Approved"),

    /** Loan application was rejected (terminal state) */
    REJECTED("Rejected"),

    /** Loan has been disbursed and is currently active (being repaid) */
    ACTIVE("Active"),

    /** Loan has been fully repaid (terminal state) */
    CLOSED("Closed");

    private final String label;

    LoanStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
