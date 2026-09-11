package com.securebank.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavingsCurrentAccountTest {

    @Test
    @DisplayName("SavingsAccount earns 4% annual interest")
    void savingsInterest() {
        SavingsAccount savings = new SavingsAccount("ACC-S-1", "A", "C1", 1000.0);
        assertEquals(40.0, savings.calculateInterest(), 1e-9);
        assertEquals(4.0, savings.getInterestRate(), 1e-9);
    }

    @Test
    @DisplayName("CurrentAccount earns 1% annual interest")
    void currentInterest() {
        CurrentAccount current = new CurrentAccount("ACC-C-1", "B", "C1", 1000.0);
        assertEquals(10.0, current.calculateInterest(), 1e-9);
        assertEquals(1.0, current.getInterestRate(), 1e-9);
    }

    @Test
    @DisplayName("CurrentAccount earns no interest on a negative (overdraft) balance")
    void currentInterestOnOverdraftIsZero() {
        CurrentAccount current = new CurrentAccount("ACC-C-2", "B", "C1", 0.0);
        current.setBalanceFromFile(-2000.0);
        assertEquals(0.0, current.calculateInterest(), 1e-9);
    }

    @Test
    @DisplayName("Runtime polymorphism: base-class reference dispatches to the subclass implementation")
    void polymorphicDispatch() {
        Account asSavings = new SavingsAccount("ACC-S-2", "A", "C1", 2000.0);
        Account asCurrent = new CurrentAccount("ACC-C-3", "B", "C1", 2000.0);

        assertEquals(80.0, asSavings.calculateInterest(), 1e-9);
        assertEquals(20.0, asCurrent.calculateInterest(), 1e-9);
        assertEquals("Savings", asSavings.getAccountType());
        assertEquals("Current", asCurrent.getAccountType());
    }

    @Test
    @DisplayName("Minimum balances differ per account type")
    void minimumBalances() {
        SavingsAccount savings = new SavingsAccount("ACC-S-3", "A", "C1", 0.0);
        CurrentAccount current = new CurrentAccount("ACC-C-4", "B", "C1", 0.0);

        assertEquals(1000.0, savings.getMinimumBalance(), 1e-9);
        assertEquals(5000.0, current.getMinimumBalance(), 1e-9);
    }

    @Test
    @DisplayName("CurrentAccount available balance includes the overdraft limit")
    void overdraftAvailableBalance() {
        CurrentAccount current = new CurrentAccount("ACC-C-5", "B", "C1", 2000.0, 10000.0);
        assertEquals(12000.0, current.getAvailableBalance(), 1e-9);

        current.setOverdraftLimit(500.0);
        assertEquals(2500.0, current.getAvailableBalance(), 1e-9);
    }

    @Test
    @DisplayName("Account constructor clamps negative initial balances to zero")
    void negativeInitialBalanceClamped() {
        SavingsAccount savings = new SavingsAccount("ACC-S-4", "A", "C1", -500.0);
        assertEquals(0.0, savings.getBalance(), 1e-9);
    }

    @Test
    @DisplayName("toString contains type, account number and balance")
    void toStringContainsKeyInfo() {
        SavingsAccount savings = new SavingsAccount("ACC-S-5", "A", "C1", 1000.0);
        String s = savings.toString();
        assertTrue(s.contains("ACC-S-5"));
        assertTrue(s.contains("1000"));
    }
}
