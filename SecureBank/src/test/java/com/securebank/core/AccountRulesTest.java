package com.securebank.core;

import com.securebank.exceptions.AccountInactiveException;
import com.securebank.exceptions.InsufficientBalanceException;
import com.securebank.transactions.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 correctness rules: overdraft, minimum balance, frozen accounts,
 * transaction typing, and remarks sanitization.
 */
class AccountRulesTest {

    // ---- Overdraft (CurrentAccount) ----

    @Test
    @DisplayName("CurrentAccount can withdraw into its overdraft limit")
    void currentAccountOverdraftWithdrawal() throws Exception {
        CurrentAccount current = new CurrentAccount("ACC-OD-1", "H", "C1", 2000.0, 10000.0);

        assertEquals(-3000.0, current.withdraw(5000.0), 1e-9);
    }

    @Test
    @DisplayName("Withdrawal beyond overdraft headroom is rejected")
    void currentAccountBeyondOverdraftRejected() {
        CurrentAccount current = new CurrentAccount("ACC-OD-2", "H", "C1", 2000.0, 10000.0);

        assertThrows(InsufficientBalanceException.class, () -> current.withdraw(12000.01));
        assertEquals(2000.0, current.getBalance(), 1e-9);
    }

    @Test
    @DisplayName("Default overdraft limit is ₹10,000")
    void defaultOverdraft() {
        CurrentAccount current = new CurrentAccount("ACC-OD-3", "H", "C1", 0.0);
        assertEquals(10000.0, current.getOverdraftLimit(), 1e-9);
    }

    // ---- Minimum balance (SavingsAccount) ----

    @Test
    @DisplayName("SavingsAccount cannot withdraw below the ₹1,000 minimum balance")
    void savingsMinimumBalanceEnforced() {
        SavingsAccount savings = new SavingsAccount("ACC-MB-1", "H", "C1", 5000.0);

        InsufficientBalanceException ex = assertThrows(InsufficientBalanceException.class,
                () -> savings.withdraw(4500.0)); // would leave 500 < 1000
        assertTrue(ex.getMessage().contains("Minimum balance"));
        assertEquals(5000.0, savings.getBalance(), 1e-9);
    }

    @Test
    @DisplayName("SavingsAccount can withdraw down to exactly the minimum")
    void savingsCanWithdrawToExactMinimum() throws Exception {
        SavingsAccount savings = new SavingsAccount("ACC-MB-2", "H", "C1", 5000.0);
        assertEquals(1000.0, savings.withdraw(4000.0), 1e-9);
    }

    @Test
    @DisplayName("Transfers respect the savings minimum balance too")
    void transferRespectsMinimumBalance() {
        SavingsAccount from = new SavingsAccount("ACC-MB-3", "H", "C1", 5000.0);
        CurrentAccount to = new CurrentAccount("ACC-MB-4", "H2", "C2", 0.0);

        assertThrows(InsufficientBalanceException.class, () -> from.transferTo(to, 4500.0));
        assertEquals(5000.0, from.getBalance(), 1e-9);
        assertEquals(0.0, to.getBalance(), 1e-9);
    }

    @Test
    @DisplayName("CurrentAccount transfers may draw into overdraft within the limit")
    void currentAccountTransferIntoOverdraft() throws Exception {
        CurrentAccount from = new CurrentAccount("ACC-MB-5", "H", "C1", 2000.0, 10000.0);
        SavingsAccount to = new SavingsAccount("ACC-MB-6", "H2", "C2", 5000.0);

        from.transferTo(to, 5000.0);

        assertEquals(-3000.0, from.getBalance(), 1e-9);
        assertEquals(10000.0, to.getBalance(), 1e-9);
    }

    // ---- Inactive accounts ----

    @Test
    @DisplayName("Deposits, withdrawals and transfers are blocked on frozen accounts")
    void inactiveAccountOperationsBlocked() throws Exception {
        SavingsAccount frozen = new SavingsAccount("ACC-FR-1", "H", "C1", 5000.0);
        SavingsAccount other = new SavingsAccount("ACC-FR-2", "H2", "C2", 5000.0);
        frozen.setActive(false);

        assertThrows(AccountInactiveException.class, () -> frozen.deposit(100.0));
        assertThrows(AccountInactiveException.class, () -> frozen.withdraw(100.0));
        assertThrows(AccountInactiveException.class, () -> frozen.transferTo(other, 100.0));

        // A frozen TARGET blocks the transfer as well
        frozen.setActive(true);
        other.setActive(false);
        assertThrows(AccountInactiveException.class, () -> frozen.transferTo(other, 100.0));

        assertEquals(5000.0, frozen.getBalance(), 1e-9);
        assertEquals(5000.0, other.getBalance(), 1e-9);
    }

    // ---- Transaction typing and remarks ----

    @Test
    @DisplayName("Deposits can be recorded with an explicit LOAN_DISBURSEMENT type")
    void depositWithExplicitType() throws Exception {
        SavingsAccount savings = new SavingsAccount("ACC-TY-1", "H", "C1", 0.0);
        savings.deposit(25000.0, "Loan disbursement - LOAN-000001",
                TransactionType.LOAN_DISBURSEMENT);

        assertEquals(TransactionType.LOAN_DISBURSEMENT,
                savings.getTransactionHistory().get(0).getType());
    }

    @Test
    @DisplayName("Remarks are sanitized: pipes stripped, long text capped at 120 chars")
    void remarksSanitized() throws Exception {
        SavingsAccount savings = new SavingsAccount("ACC-SR-1", "H", "C1", 0.0);

        savings.deposit(10.0, "bad|pipe|remarks");
        assertEquals("bad/pipe/remarks",
                savings.getTransactionHistory().get(0).getRemarks());

        String longText = "x".repeat(300);
        savings.deposit(10.0, longText);
        assertEquals(120, savings.getTransactionHistory().get(1).getRemarks().length());

        // Null remarks remain safe
        savings.deposit(10.0, null);
        assertEquals("", savings.getTransactionHistory().get(2).getRemarks());
    }
}
