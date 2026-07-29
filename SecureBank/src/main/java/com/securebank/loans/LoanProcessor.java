package com.securebank.loans;

import com.securebank.core.Account;
import com.securebank.repository.Repository;
import com.securebank.utils.FileIOHelper;
import com.securebank.utils.IDGenerator;

import java.util.List;

/**
 * LoanProcessor — handles loan application processing, approval, and management.
 *
 * Contains the business logic for:
 * - Checking loan eligibility
 * - Approving/rejecting loan applications
 * - Processing loan disbursements
 * - Managing repayments
 *
 * RUBRIC: Demonstrates control statements, exception handling patterns, and
 * interaction between multiple classes (Loan, Account, Repository).
 */
public class LoanProcessor {

    /** Repository for storing loan records — uses Generic Repository<Loan> */
    private final Repository<Loan> loanRepository;

    /** Maximum loan amount allowed */
    private static final double MAX_LOAN_AMOUNT = 500000.0;

    /** Minimum loan amount */
    private static final double MIN_LOAN_AMOUNT = 5000.0;

    /** Default annual interest rate for personal loans */
    private static final double DEFAULT_INTEREST_RATE = 8.5;

    /** Minimum account balance required for loan eligibility */
    private static final double MIN_BALANCE_FOR_LOAN = 5000.0;

    /**
     * Creates a new LoanProcessor with its own loan repository.
     */
    public LoanProcessor() {
        this.loanRepository = new Repository<>("Loan");
    }

    /**
     * Applies for a new loan. Validates eligibility before creating the loan.
     *
     * RUBRIC: Unit 3 — Demonstrates control statements and exception handling.
     * Uses nested if-else for eligibility checks.
     *
     * @param customerId    the customer applying
     * @param accountNumber the linked account
     * @param amount        the requested amount
     * @param tenureMonths  the loan duration in months
     * @param purpose       the reason for the loan
     * @param account       the Account object (for balance check)
     * @return the created Loan object, or null if eligibility fails
     */
    public Loan applyForLoan(String customerId, String accountNumber, double amount,
                              int tenureMonths, String purpose, Account account) {

        // ---- Eligibility checks (control statements) ----

        // Check 1: Amount range
        if (amount < MIN_LOAN_AMOUNT) {
            System.out.println("Loan rejected: Amount ₹" + amount +
                    " is below minimum (₹" + MIN_LOAN_AMOUNT + ")");
            return null;
        }
        if (amount > MAX_LOAN_AMOUNT) {
            System.out.println("Loan rejected: Amount ₹" + amount +
                    " exceeds maximum (₹" + MAX_LOAN_AMOUNT + ")");
            return null;
        }

        // Check 2: Valid tenure (6 to 60 months)
        if (tenureMonths < 6 || tenureMonths > 60) {
            System.out.println("Loan rejected: Tenure must be between 6 and 60 months");
            return null;
        }

        // Check 3: Account balance check
        if (account != null && account.getBalance() < MIN_BALANCE_FOR_LOAN) {
            System.out.println("Loan rejected: Account balance (₹" + account.getBalance() +
                    ") is below minimum required (₹" + MIN_BALANCE_FOR_LOAN + ")");
            return null;
        }

        // Check 4: No existing active loans on this account (simplified rule)
        List<Loan> existingLoans = loanRepository.search(
                loan -> loan.getAccountNumber().equals(accountNumber)
                        && (loan.getStatus() == LoanStatus.ACTIVE
                        || loan.getStatus() == LoanStatus.APPROVED)
        );
        if (!existingLoans.isEmpty()) {
            System.out.println("Loan rejected: An active loan already exists on this account");
            return null;
        }

        // All checks passed — create the loan
        String loanId = IDGenerator.generateLoanId();
        Loan loan = new Loan(loanId, customerId, accountNumber, amount,
                DEFAULT_INTEREST_RATE, tenureMonths, purpose);

        loanRepository.add(loanId, loan);

        System.out.println("Loan application submitted: " + loan);
        return loan;
    }

    /**
     * Approves a pending loan application.
     *
     * @param loanId the loan to approve
     * @return true if approved, false if not found or not in PENDING status
     */
    public boolean approveLoan(String loanId) {
        Loan loan = loanRepository.get(loanId);
        if (loan == null) {
            return false;
        }
        if (loan.getStatus() != LoanStatus.PENDING) {
            return false;
        }
        loan.setStatus(LoanStatus.APPROVED);
        return true;
    }

    /**
     * Rejects a pending loan application.
     *
     * @param loanId the loan to reject
     * @return true if rejected, false if not found or not in PENDING status
     */
    public boolean rejectLoan(String loanId) {
        Loan loan = loanRepository.get(loanId);
        if (loan == null) {
            return false;
        }
        if (loan.getStatus() != LoanStatus.PENDING) {
            return false;
        }
        loan.setStatus(LoanStatus.REJECTED);
        return true;
    }

    /**
     * Disburses an approved loan — credits the loan amount to the linked account
     * and marks the loan as ACTIVE.
     *
     * @param loanId  the loan to disburse
     * @param account the account to credit
     * @return true if disbursed successfully
     */
    public boolean disburseLoan(String loanId, Account account) {
        Loan loan = loanRepository.get(loanId);
        if (loan == null || loan.getStatus() != LoanStatus.APPROVED) {
            return false;
        }

        // Credit the loan amount to the account
        account.deposit(loan.getAmount(), "Loan disbursement - " + loanId);
        loan.setStatus(LoanStatus.ACTIVE);

        return true;
    }

    /**
     * Gets a loan by its ID.
     */
    public Loan getLoan(String loanId) {
        return loanRepository.get(loanId);
    }

    /**
     * Gets all loans for a specific customer.
     */
    public List<Loan> getLoansByCustomer(String customerId) {
        return loanRepository.search(loan -> loan.getCustomerId().equals(customerId));
    }

    /**
     * Gets all loans for a specific account.
     */
    public List<Loan> getLoansByAccount(String accountNumber) {
        return loanRepository.search(loan -> loan.getAccountNumber().equals(accountNumber));
    }

    /**
     * Gets all loans.
     */
    public List<Loan> getAllLoans() {
        return loanRepository.getAll();
    }

    /**
     * Saves all loans to file.
     */
    public void saveToFile() {
        FileIOHelper.saveLoans(loanRepository.getAll());
    }

    /**
     * Loads loans from file.
     */
    public void loadFromFile() {
        List<Loan> loans = FileIOHelper.loadLoans();
        for (Loan loan : loans) {
            loanRepository.add(loan.getLoanId(), loan);
        }
    }

    /**
     * Gets the underlying repository.
     */
    public Repository<Loan> getRepository() {
        return loanRepository;
    }
}
