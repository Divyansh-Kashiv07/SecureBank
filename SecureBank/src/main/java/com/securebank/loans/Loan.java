package com.securebank.loans;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Loan — represents a loan issued to a customer against a specific account.
 *
 * Contains all details about the loan: amount, interest rate, tenure, EMI,
 * status, and repayment progress.
 *
 * RUBRIC: Demonstrates constructors, control statements, and clean OOP design.
 */
public class Loan {

    /** Date format for persistence */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Unique loan identifier (e.g., "LOAN-000001") */
    private final String loanId;

    /** Customer ID who applied for the loan */
    private final String customerId;

    /** Account number linked to this loan (for disbursement and repayment) */
    private final String accountNumber;

    /** Principal loan amount requested */
    private final double amount;

    /** Annual interest rate (e.g., 8.5 for 8.5%) */
    private final double interestRate;

    /** Loan tenure in months (e.g., 12, 24, 36, 60) */
    private final int tenureMonths;

    /** Calculated Equated Monthly Installment */
    private double emi;

    /** Total amount to be repaid (principal + interest) */
    private double totalRepayable;

    /** Amount already repaid */
    private double amountRepaid;

    /** Current status of the loan */
    private LoanStatus status;

    /** When the loan was applied for */
    private final LocalDateTime appliedAt;

    /** Purpose/reason for the loan (e.g., "Home improvement", "Education") */
    private final String purpose;

    /**
     * Creates a new loan application.
     *
     * @param loanId        unique loan identifier
     * @param customerId    the customer applying
     * @param accountNumber the linked account
     * @param amount        principal amount
     * @param interestRate  annual interest rate (percentage)
     * @param tenureMonths  duration in months
     * @param purpose       reason for the loan
     */
    public Loan(String loanId, String customerId, String accountNumber,
                double amount, double interestRate, int tenureMonths, String purpose) {
        this.loanId = loanId;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.interestRate = interestRate;
        this.tenureMonths = tenureMonths;
        this.purpose = purpose;
        this.status = LoanStatus.PENDING;
        this.appliedAt = LocalDateTime.now();
        this.amountRepaid = 0;

        // Calculate EMI and total repayable amount
        calculateEMI();
    }

    /**
     * Calculates EMI using the standard reducing balance formula.
     *
     * Formula: EMI = [P × R × (1+R)^N] / [(1+R)^N - 1]
     * Where:
     *   P = Principal amount
     *   R = Monthly interest rate (annual rate / 12 / 100)
     *   N = Number of months (tenure)
     *
     * VIVA NOTE: This is a standard banking formula. The key insight is that
     * R is the MONTHLY rate, not annual. So 12% annual = 1% monthly = 0.01.
     */
    private void calculateEMI() {
        if (interestRate == 0) {
            // Zero interest loan — simple division
            this.emi = amount / tenureMonths;
            this.totalRepayable = amount;
            return;
        }

        double monthlyRate = interestRate / 12.0 / 100.0;  // Convert annual % to monthly decimal
        double power = Math.pow(1 + monthlyRate, tenureMonths);

        // EMI formula
        this.emi = (amount * monthlyRate * power) / (power - 1);
        this.totalRepayable = this.emi * tenureMonths;
    }

    /**
     * Records a repayment against this loan.
     *
     * @param repaymentAmount the amount being repaid
     */
    public void makeRepayment(double repaymentAmount) {
        this.amountRepaid += repaymentAmount;

        // Check if loan is fully repaid
        if (amountRepaid >= totalRepayable) {
            this.status = LoanStatus.CLOSED;
        }
    }

    /**
     * Returns the remaining amount to be repaid.
     * @return outstanding balance
     */
    public double getOutstandingAmount() {
        return Math.max(0, totalRepayable - amountRepaid);
    }

    // ==================== SERIALIZATION ====================

    /**
     * Converts to pipe-delimited string for file persistence.
     */
    public String toFileString() {
        StringBuilder sb = new StringBuilder();
        sb.append(loanId).append("|");
        sb.append(customerId).append("|");
        sb.append(accountNumber).append("|");
        sb.append(String.format("%.2f", amount)).append("|");
        sb.append(String.format("%.2f", interestRate)).append("|");
        sb.append(tenureMonths).append("|");
        sb.append(String.format("%.2f", emi)).append("|");
        sb.append(String.format("%.2f", totalRepayable)).append("|");
        sb.append(String.format("%.2f", amountRepaid)).append("|");
        sb.append(status.name()).append("|");
        sb.append(appliedAt.format(DATE_FORMAT)).append("|");
        sb.append(purpose);
        return sb.toString();
    }

    /**
     * Parses a loan from a pipe-delimited file string.
     */
    public static Loan fromFileString(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 11) return null;

            Loan loan = new Loan(
                    parts[0],                       // loanId
                    parts[1],                       // customerId
                    parts[2],                       // accountNumber
                    Double.parseDouble(parts[3]),   // amount
                    Double.parseDouble(parts[4]),   // interestRate
                    Integer.parseInt(parts[5]),      // tenureMonths
                    parts.length > 11 ? parts[11] : ""  // purpose
            );

            // Restore calculated/saved fields
            loan.emi = Double.parseDouble(parts[6]);
            loan.totalRepayable = Double.parseDouble(parts[7]);
            loan.amountRepaid = Double.parseDouble(parts[8]);
            loan.status = LoanStatus.valueOf(parts[9]);

            return loan;
        } catch (Exception e) {
            System.err.println("Warning: Could not parse loan line: " + line);
            return null;
        }
    }

    // ==================== GETTERS ====================

    public String getLoanId() { return loanId; }
    public String getCustomerId() { return customerId; }
    public String getAccountNumber() { return accountNumber; }
    public double getAmount() { return amount; }
    public double getInterestRate() { return interestRate; }
    public int getTenureMonths() { return tenureMonths; }
    public double getEmi() { return emi; }
    public double getTotalRepayable() { return totalRepayable; }
    public double getAmountRepaid() { return amountRepaid; }
    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public String getPurpose() { return purpose; }

    @Override
    public String toString() {
        return String.format("Loan [%s] — ₹%.2f at %.1f%% for %d months — EMI: ₹%.2f — Status: %s",
                loanId, amount, interestRate, tenureMonths, emi, status.getLabel());
    }
}
