package com.securebank.core;

import com.securebank.exceptions.DailyLimitExceededException;
import com.securebank.exceptions.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {

    private SavingsAccount account;

    @BeforeEach
    void setUp() {
        // Seed above the savings minimum balance (₹1,000) so withdrawals are
        // allowed while still testing normal operation
        account = new SavingsAccount("ACC-TEST-001", "Test Holder", "CUSTOMER-TEST", 5000.0);
    }

    @Test
    @DisplayName("Deposit increases balance and returns the new balance")
    void depositIncreasesBalance() throws Exception {
        double newBalance = account.deposit(500.0);

        assertEquals(5500.0, newBalance, 1e-9);
        assertEquals(5500.0, account.getBalance(), 1e-9);
    }

    @Test
    @DisplayName("Deposit with remarks records the remark in history")
    void depositWithRemarks() throws Exception {
        account.deposit(250.0, "Salary credit");

        assertEquals("Salary credit",
                account.getTransactionHistory().get(0).getRemarks());
    }

    @Test
    @DisplayName("Deposit rejects zero and negative amounts")
    void depositRejectsInvalidAmounts() {
        assertThrows(IllegalArgumentException.class, () -> account.deposit(0));
        assertThrows(IllegalArgumentException.class, () -> account.deposit(-100));
    }

    @Test
    @DisplayName("Withdraw decreases balance and records a WITHDRAWAL transaction")
    void withdrawDecreasesBalance() throws Exception {
        double newBalance = account.withdraw(400.0);

        assertEquals(4600.0, newBalance, 1e-9);
        assertEquals(1, account.getTransactionHistory().size());
        assertEquals("WITHDRAWAL",
                account.getTransactionHistory().get(0).getType().name());
    }

    @Test
    @DisplayName("Withdraw violating the balance rule throws InsufficientBalanceException and leaves balance unchanged")
    void withdrawBeyondBalanceThrows() {
        // Seed 5000, savings minimum 1000 → anything above 4000 is rejected
        assertThrows(InsufficientBalanceException.class, () -> account.withdraw(4500.0));
        assertEquals(5000.0, account.getBalance(), 1e-9);
    }

    @Test
    @DisplayName("Withdraw rejects zero and negative amounts")
    void withdrawRejectsInvalidAmounts() {
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(0));
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(-50));
    }

    @Test
    @DisplayName("Daily withdrawal limit is enforced, with the limit boundary itself allowed")
    void dailyLimitEnforced() throws Exception {
        account.setDailyLimit(100.0);

        assertDoesNotThrow(() -> account.withdraw(60.0));
        // 60 + 50 would exceed the 100 limit
        assertThrows(DailyLimitExceededException.class, () -> account.withdraw(50.0));
        // Exactly reaching the limit is allowed
        assertDoesNotThrow(() -> account.withdraw(40.0));

        assertEquals(4900.0, account.getBalance(), 1e-9);
        assertEquals(100.0, account.getTodayWithdrawnTotal(), 1e-9);
    }

    @Test
    @DisplayName("toFileString produces the documented 7-field pipe format")
    void toFileStringFormat() {
        String line = account.toFileString();
        String[] parts = line.split("\\|", -1);

        assertEquals(7, parts.length);
        assertEquals("ACC-TEST-001", parts[0]);
        assertEquals("Test Holder", parts[1]);
        assertEquals("CUSTOMER-TEST", parts[2]);
        assertEquals("5000.00", parts[3]);
        assertEquals("Savings", parts[4]);
        assertEquals("50000.00", parts[5]);
        assertEquals("true", parts[6]);
    }

    @Test
    @DisplayName("Deposit history grows with each operation and defensive copy is returned")
    void transactionHistoryIsDefensivelyCopied() throws Exception {
        account.deposit(100.0);
        account.getTransactionHistory().clear();

        assertEquals(1, account.getTransactionHistory().size());
        assertTrue(account.getCreatedAt() != null);
    }
}
