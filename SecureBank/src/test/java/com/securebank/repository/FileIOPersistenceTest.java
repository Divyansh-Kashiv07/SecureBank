package com.securebank.repository;

import com.securebank.core.Account;
import com.securebank.core.CurrentAccount;
import com.securebank.core.Customer;
import com.securebank.core.SavingsAccount;
import com.securebank.loans.Loan;
import com.securebank.utils.FileIOHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileIOPersistenceTest {

    @BeforeEach
    void clearAllDataFiles() {
        // Files land in target/test-data (configured via securebank.data.dir in the pom)
        File dir = new File(System.getProperty("securebank.data.dir", "target/test-data"));
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
    }

    @Test
    @DisplayName("Account round-trip preserves type, balance, daily limit, active flag and history")
    void accountRoundTrip() throws Exception {
        SavingsAccount savings = new SavingsAccount("ACC-P-1", "Alice", "C1", 5000.0);
        CurrentAccount current = new CurrentAccount("ACC-P-2", "Bob", "C2", 8000.0, 15000.0);
        current.setDailyLimit(25000.0);
        current.setActive(false);
        savings.deposit(500.0, "round trip deposit");

        FileIOHelper.saveAccounts(List.of(savings, current));
        List<Account> loaded = FileIOHelper.loadAccounts();

        assertEquals(2, loaded.size());
        Account loadedSavings = loaded.stream()
                .filter(a -> a.getAccountNumber().equals("ACC-P-1")).findFirst().orElseThrow();
        Account loadedCurrent = loaded.stream()
                .filter(a -> a.getAccountNumber().equals("ACC-P-2")).findFirst().orElseThrow();

        assertTrue(loadedSavings instanceof SavingsAccount);
        assertEquals(5500.0, loadedSavings.getBalance(), 1e-9);
        assertTrue(loadedCurrent instanceof CurrentAccount);
        assertEquals(8000.0, loadedCurrent.getBalance(), 1e-9);
        // Overdraft limit now persists (Phase 2 fix)
        assertEquals(15000.0, ((CurrentAccount) loadedCurrent).getOverdraftLimit(), 1e-9);
        assertEquals(25000.0, loadedCurrent.getDailyLimit(), 1e-9);
        assertFalse(loadedCurrent.isActive());
        // Transaction history round-trips too (per-account txn files)
        assertEquals(1, loadedSavings.getTransactionHistory().size());
        assertEquals("round trip deposit",
                loadedSavings.getTransactionHistory().get(0).getRemarks());
    }

    @Test
    @DisplayName("Customer round-trip preserves all fields and account links")
    void customerRoundTrip() throws Exception {
        Customer customer = new Customer("CUSTOMER-P1", "Persisted Person",
                "p@securebank.com", "9111111111", "Persist City", "4321");
        customer.addAccount("ACC-P-1");

        FileIOHelper.saveCustomers(List.of(customer));
        List<Customer> loaded = FileIOHelper.loadCustomers();

        assertEquals(1, loaded.size());
        Customer parsed = loaded.get(0);
        assertEquals("CUSTOMER-P1", parsed.getCustomerId());
        assertEquals("Persisted Person", parsed.getName());
        assertTrue(parsed.validatePin("4321"));
        assertEquals(List.of("ACC-P-1"), parsed.getAccountNumbers());
    }

    @Test
    @DisplayName("Loan round-trip preserves EMI, status and repayment state")
    void loanRoundTrip() throws Exception {
        Loan loan = new Loan("LOAN-P-1", "CUSTOMER-P1", "ACC-P-1",
                100000.0, 8.5, 24, "Home improvement");
        loan.makeRepayment(5000.0);

        FileIOHelper.saveLoans(List.of(loan));
        List<Loan> loaded = FileIOHelper.loadLoans();

        assertEquals(1, loaded.size());
        Loan parsed = loaded.get(0);
        assertEquals("LOAN-P-1", parsed.getLoanId());
        assertEquals(100000.0, parsed.getAmount(), 1e-9);
        assertEquals(8.5, parsed.getInterestRate(), 1e-9);
        assertEquals(24, parsed.getTenureMonths());
        assertEquals(5000.0, parsed.getAmountRepaid(), 1e-9);
        assertEquals(loan.getEmi(), parsed.getEmi(), 0.01); // persisted with 2 decimals
        assertEquals(loan.getTotalRepayable(), parsed.getTotalRepayable(), 0.01);
        assertEquals(loan.getStatus(), parsed.getStatus());
        // appliedAt now restored from file (Phase 2 fix). The file format stores
        // timestamps to whole seconds, so compare at that precision.
        assertEquals(loan.getAppliedAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
                parsed.getAppliedAt());
    }
}
