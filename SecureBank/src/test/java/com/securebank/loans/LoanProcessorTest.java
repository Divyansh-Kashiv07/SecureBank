package com.securebank.loans;

import com.securebank.core.Account;
import com.securebank.core.CurrentAccount;
import com.securebank.core.SavingsAccount;
import com.securebank.utils.FileIOHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanProcessorTest {

    private LoanProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new LoanProcessor();
    }

    @Test
    @DisplayName("EMI uses the standard reducing-balance formula")
    void emiFormula() {
        Loan loan = new Loan("LOAN-EMI-1", "C1", "ACC-1", 100000.0, 12.0, 12, "Test");
        // P=100000, monthly R=1% (0.01), N=12 → EMI ≈ 8884.88
        assertEquals(8884.88, loan.getEmi(), 0.01);
        assertEquals(loan.getEmi() * 12, loan.getTotalRepayable(), 0.01);
    }

    @Test
    @DisplayName("Zero-interest loan divides the principal evenly")
    void zeroInterestEmi() {
        Loan loan = new Loan("LOAN-EMI-2", "C1", "ACC-1", 12000.0, 0.0, 12, "Test");
        assertEquals(1000.0, loan.getEmi(), 1e-9);
        assertEquals(12000.0, loan.getTotalRepayable(), 1e-9);
    }

    @Test
    @DisplayName("Valid application is created PENDING with a generated ID")
    void validApplication() {
        Account account = new SavingsAccount("ACC-L-1", "H", "C1", 25000.0);

        Loan loan = processor.applyForLoan("C1", "ACC-L-1", 50000.0, 24, "Education", account);

        assertNotNull(loan);
        assertEquals(LoanStatus.PENDING, loan.getStatus());
        assertTrue(loan.getLoanId().startsWith("LOAN-"));
        assertEquals(8.5, loan.getInterestRate(), 1e-9);
    }

    @Test
    @DisplayName("Applications outside amount/tenure/balance eligibility are rejected")
    void eligibilityRejections() {
        Account poorAccount = new SavingsAccount("ACC-L-2", "H", "C1", 100.0);

        assertNull(processor.applyForLoan("C1", "ACC-L-2", 1000.0, 12, "Below min", poorAccount));
        assertNull(processor.applyForLoan("C1", "ACC-L-2", 999999.0, 12, "Above max", poorAccount));
        assertNull(processor.applyForLoan("C1", "ACC-L-2", 50000.0, 3, "Tenure too short", poorAccount));
        assertNull(processor.applyForLoan("C1", "ACC-L-2", 50000.0, 72, "Tenure too long", poorAccount));
        assertNull(processor.applyForLoan("C1", "ACC-L-2", 50000.0, 12, "Balance too low", poorAccount));
        assertEquals(0, processor.getAllLoans().size());
    }

    @Test
    @DisplayName("Only one active/approved loan per account at a time")
    void oneActiveLoanPerAccount() {
        Account account = new SavingsAccount("ACC-L-3", "H", "C1", 50000.0);

        Loan first = processor.applyForLoan("C1", "ACC-L-3", 50000.0, 12, "First", account);
        assertNotNull(first);
        processor.approveLoan(first.getLoanId());

        Loan second = processor.applyForLoan("C1", "ACC-L-3", 20000.0, 12, "Second", account);
        assertNull(second);
    }

    @Test
    @DisplayName("Approve/reject only work on PENDING loans")
    void approveRejectTransitions() {
        Account account = new SavingsAccount("ACC-L-4", "H", "C1", 25000.0);
        Loan loan = processor.applyForLoan("C1", "ACC-L-4", 20000.0, 12, "Test", account);
        assertNotNull(loan);

        assertTrue(processor.approveLoan(loan.getLoanId()));
        assertFalse(processor.approveLoan(loan.getLoanId()));   // Already approved
        assertFalse(processor.rejectLoan(loan.getLoanId()));    // Not pending anymore

        Loan rejected = processor.applyForLoan("C1", "ACC-L-4", 20000.0, 12, "Test", account);
        assertNull(rejected); // approved loan still counts as active
    }

    @Test
    @DisplayName("Disbursement credits the linked account and activates the loan")
    void disbursement() {
        Account account = new SavingsAccount("ACC-L-5", "H", "C1", 25000.0);
        Loan loan = processor.applyForLoan("C1", "ACC-L-5", 50000.0, 12, "Disburse me", account);
        assertNotNull(loan);
        processor.approveLoan(loan.getLoanId());

        double before = account.getBalance();
        assertTrue(processor.disburseLoan(loan.getLoanId(), account));
        assertEquals(LoanStatus.ACTIVE, loan.getStatus());
        assertEquals(before + 50000.0, account.getBalance(), 1e-9);
        assertEquals(1, account.getTransactionHistory().size());
        // Disbursements are recorded with the dedicated LOAN_DISBURSEMENT type (Phase 2 fix)
        assertEquals("LOAN_DISBURSEMENT",
                account.getTransactionHistory().get(0).getType().name());
    }

    @Test
    @DisplayName("Disbursement fails for a non-approved or unknown loan")
    void disbursementGuards() {
        Account account = new CurrentAccount("ACC-L-6", "H", "C1", 25000.0);
        assertFalse(processor.disburseLoan("LOAN-UNKNOWN", account));

        Loan loan = processor.applyForLoan("C1", "ACC-L-6", 50000.0, 12, "Test", account);
        assertNotNull(loan);
        assertFalse(processor.disburseLoan(loan.getLoanId(), account)); // still PENDING
    }

    @Test
    @DisplayName("Repayments accumulate and close the loan when fully repaid")
    void repaymentLifecycle() {
        Account account = new SavingsAccount("ACC-L-7", "H", "C1", 25000.0);
        Loan loan = processor.applyForLoan("C1", "ACC-L-7", 50000.0, 12, "Repay me", account);
        assertNotNull(loan);
        processor.approveLoan(loan.getLoanId());
        processor.disburseLoan(loan.getLoanId(), account);

        loan.makeRepayment(loan.getTotalRepayable() / 2);
        assertEquals(LoanStatus.ACTIVE, loan.getStatus());
        assertEquals(loan.getTotalRepayable() / 2, loan.getAmountRepaid(), 1e-9);

        loan.makeRepayment(loan.getTotalRepayable());
        assertEquals(LoanStatus.CLOSED, loan.getStatus());
        assertEquals(0.0, loan.getOutstandingAmount(), 1e-9);
    }

    @Test
    @DisplayName("Loans persist to and load from the isolated data directory")
    void loanPersistenceRoundTrip() throws Exception {
        File dataDir = new File(System.getProperty("securebank.data.dir", "target/test-data"));
        if (dataDir.exists()) {
            for (File f : dataDir.listFiles()) f.delete();
        }

        Account account = new SavingsAccount("ACC-L-8", "H", "C1", 25000.0);
        Loan loan = processor.applyForLoan("C1", "ACC-L-8", 50000.0, 12, "Persist", account);
        assertNotNull(loan);
        processor.saveToFile();

        LoanProcessor fresh = new LoanProcessor();
        fresh.loadFromFile();
        assertEquals(1, fresh.getAllLoans().size());
        assertEquals(loan.getLoanId(), fresh.getAllLoans().get(0).getLoanId());
        assertEquals(loan.getEmi(), fresh.getAllLoans().get(0).getEmi(), 0.01); // persisted with 2 decimals
    }
}
