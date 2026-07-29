package com.securebank.core;

import com.securebank.exceptions.DailyLimitExceededException;
import com.securebank.exceptions.InsufficientBalanceException;
import com.securebank.transactions.Transaction;
import com.securebank.transactions.TransactionType;
import com.securebank.utils.IDGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Account — the ABSTRACT base class for all bank accounts in SecureBank.
 *
 * RUBRIC COVERAGE (this is one of the most important classes for your viva):
 * - Unit 1: Abstract class, Composition (owns Transactions), Association (linked to Customer)
 * - Unit 2: Abstract method calculateInterest(), Method Overloading (deposit)
 * - Unit 4: synchronized keyword on deposit() and withdraw() for thread safety
 * - Unit 5: ArrayList<Transaction> for transaction history
 *
 * VIVA NOTE — WHY SYNCHRONIZED?
 * In our client-server architecture, multiple clients can connect simultaneously.
 * Each client runs in its own thread (ClientHandler). If two clients try to
 * withdraw from the SAME account at the SAME time without synchronization:
 *
 *   Thread A reads balance = 1000
 *   Thread B reads balance = 1000       ← Both see 1000!
 *   Thread A withdraws 800 → balance = 200
 *   Thread B withdraws 800 → balance = 200  ← WRONG! Should have been rejected!
 *
 * The 'synchronized' keyword ensures only ONE thread can execute the method at a time
 * on the SAME Account object. Thread B must WAIT until Thread A finishes.
 * This is called a "monitor lock" — every Java object has one built-in.
 */
public abstract class Account implements Transferable {

    /** Unique account number (e.g., "ACC-000001") */
    private final String accountNumber;

    /** Name of the account holder (denormalized for quick display) */
    private String holderName;

    /** Customer ID this account belongs to — Association relationship */
    private final String customerId;

    /** Current balance — modified only through synchronized deposit/withdraw */
    private double balance;

    /**
     * COMPOSITION: Transaction list is OWNED by this Account.
     * If the Account is deleted, its transactions are deleted too.
     * This is different from Association (Customer-Account) where both can exist independently.
     */
    private final List<Transaction> transactionHistory;

    /** Daily withdrawal/transfer limit (default ₹50,000) */
    private double dailyLimit;

    /** Tracks total amount withdrawn/transferred today for limit enforcement */
    private double todayWithdrawnTotal;

    /** The date of the last withdrawal — used to reset daily counter */
    private LocalDate lastWithdrawalDate;

    /** Account creation timestamp */
    private final LocalDateTime createdAt;

    /** Whether this account is active (false = frozen/closed) */
    private boolean active;

    /**
     * Protected constructor — only subclasses (SavingsAccount, CurrentAccount) can call this.
     * Cannot instantiate Account directly because it's abstract.
     *
     * @param accountNumber unique account identifier
     * @param holderName    name of the account holder
     * @param customerId    the customer ID this account belongs to
     * @param initialBalance starting balance (must be >= 0)
     */
    protected Account(String accountNumber, String holderName, String customerId,
                      double initialBalance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.customerId = customerId;
        this.balance = Math.max(0, initialBalance); // Never allow negative initial balance
        this.transactionHistory = new ArrayList<>();
        this.dailyLimit = 50000.0;
        this.todayWithdrawnTotal = 0;
        this.lastWithdrawalDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    // ==================== ABSTRACT METHOD ====================

    /**
     * Calculates interest for this account type.
     * RUBRIC: Unit 2 — Abstract method. SavingsAccount and CurrentAccount
     * each provide their own implementation (Method Overriding).
     *
     * @return the calculated interest amount based on current balance
     */
    public abstract double calculateInterest();

    /**
     * Returns the account type as a display-friendly string.
     * Each subclass overrides this.
     *
     * @return "Savings" or "Current"
     */
    public abstract String getAccountType();

    // ==================== SYNCHRONIZED DEPOSIT — THREAD-SAFE ====================

    /**
     * Deposits money into this account.
     * RUBRIC: Unit 2 — Method Overloading (this is version 1: amount only).
     * RUBRIC: Unit 4 — synchronized for thread safety.
     *
     * WHY SYNCHRONIZED: The deposit operation involves:
     *   1. Read current balance
     *   2. Add amount
     *   3. Write new balance
     * If two threads do this simultaneously, one deposit could be LOST (lost update problem).
     * synchronized ensures atomic execution — only one thread at a time.
     *
     * @param amount the amount to deposit (must be positive)
     * @return the new balance after deposit
     */
    public synchronized double deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive. Got: " + amount);
        }

        // Update balance atomically (protected by synchronized)
        this.balance += amount;

        // Record the transaction — Composition: transaction belongs to this account
        Transaction txn = new Transaction(
                IDGenerator.generateTransactionId(),
                this.accountNumber,
                TransactionType.DEPOSIT,
                amount,
                this.balance,
                LocalDateTime.now(),
                ""
        );
        this.transactionHistory.add(txn);

        return this.balance;
    }

    /**
     * Deposits money with remarks.
     * RUBRIC: Unit 2 — Method Overloading (this is version 2: amount + remarks).
     *
     * Same method name 'deposit' but different parameter list — the compiler knows
     * which to call based on the arguments provided. This is compile-time polymorphism.
     *
     * @param amount  the amount to deposit
     * @param remarks description of the deposit (e.g., "Salary credit")
     * @return the new balance after deposit
     */
    public synchronized double deposit(double amount, String remarks) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive. Got: " + amount);
        }

        this.balance += amount;

        Transaction txn = new Transaction(
                IDGenerator.generateTransactionId(),
                this.accountNumber,
                TransactionType.DEPOSIT,
                amount,
                this.balance,
                LocalDateTime.now(),
                remarks
        );
        this.transactionHistory.add(txn);

        return this.balance;
    }

    // ==================== SYNCHRONIZED WITHDRAW — THREAD-SAFE ====================

    /**
     * Withdraws money from this account with full validation.
     * RUBRIC: Unit 4 — synchronized for thread safety.
     * RUBRIC: Unit 3 — throws custom checked exceptions.
     *
     * Validation order:
     * 1. Check amount is positive
     * 2. Check daily limit not exceeded
     * 3. Check sufficient balance
     * 4. Execute withdrawal
     *
     * @param amount the amount to withdraw (must be positive)
     * @return the new balance after withdrawal
     * @throws InsufficientBalanceException if balance is too low
     * @throws DailyLimitExceededException  if daily limit would be exceeded
     */
    public synchronized double withdraw(double amount)
            throws InsufficientBalanceException, DailyLimitExceededException {

        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive. Got: " + amount);
        }

        // Reset daily counter if it's a new day
        resetDailyLimitIfNewDay();

        // Check daily limit FIRST (before balance check)
        if (todayWithdrawnTotal + amount > dailyLimit) {
            throw new DailyLimitExceededException(dailyLimit, todayWithdrawnTotal, amount);
        }

        // Check sufficient balance
        if (amount > balance) {
            throw new InsufficientBalanceException(amount, balance);
        }

        // Execute withdrawal — we're inside synchronized, so this is atomic
        this.balance -= amount;
        this.todayWithdrawnTotal += amount;

        // Record transaction
        Transaction txn = new Transaction(
                IDGenerator.generateTransactionId(),
                this.accountNumber,
                TransactionType.WITHDRAWAL,
                amount,
                this.balance,
                LocalDateTime.now(),
                ""
        );
        this.transactionHistory.add(txn);

        return this.balance;
    }

    // ==================== TRANSFERABLE INTERFACE IMPLEMENTATION ====================

    /**
     * Transfers funds from this account to the target account.
     * RUBRIC: Unit 2 — Interface implementation (Transferable).
     *
     * IMPORTANT THREAD-SAFETY NOTE:
     * This method synchronizes on BOTH accounts (source and target) to prevent
     * deadlocks and race conditions during transfers. We always lock the account
     * with the SMALLER account number first to prevent deadlock.
     *
     * VIVA NOTE — DEADLOCK PREVENTION:
     * If Thread A transfers from Account1 to Account2, and Thread B transfers
     * from Account2 to Account1 simultaneously:
     *   Thread A locks Account1, tries to lock Account2
     *   Thread B locks Account2, tries to lock Account1
     *   → DEADLOCK! Both threads wait forever.
     *
     * Solution: Always acquire locks in the SAME ORDER (by account number).
     * This is called "lock ordering" and is a standard deadlock prevention technique.
     */
    @Override
    public void transferTo(Account target, double amount)
            throws InsufficientBalanceException, DailyLimitExceededException {

        if (target == null) {
            throw new IllegalArgumentException("Target account cannot be null");
        }
        if (this.accountNumber.equals(target.getAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        // Determine lock ordering to prevent deadlock
        Account firstLock, secondLock;
        if (this.accountNumber.compareTo(target.accountNumber) < 0) {
            firstLock = this;
            secondLock = target;
        } else {
            firstLock = target;
            secondLock = this;
        }

        // Acquire locks in consistent order
        synchronized (firstLock) {
            synchronized (secondLock) {
                // Reset daily counter if new day
                this.resetDailyLimitIfNewDay();

                // Check daily limit
                if (this.todayWithdrawnTotal + amount > this.dailyLimit) {
                    throw new DailyLimitExceededException(dailyLimit, todayWithdrawnTotal, amount);
                }

                // Check balance
                if (amount > this.balance) {
                    throw new InsufficientBalanceException(amount, this.balance);
                }

                // Execute transfer — debit source
                this.balance -= amount;
                this.todayWithdrawnTotal += amount;

                // Record outgoing transaction on source
                Transaction txnOut = new Transaction(
                        IDGenerator.generateTransactionId(),
                        this.accountNumber,
                        TransactionType.TRANSFER_OUT,
                        amount,
                        this.balance,
                        LocalDateTime.now(),
                        "Transfer to " + target.accountNumber
                );
                this.transactionHistory.add(txnOut);

                // Execute transfer — credit target
                target.balance += amount;

                // Record incoming transaction on target
                Transaction txnIn = new Transaction(
                        IDGenerator.generateTransactionId(),
                        target.accountNumber,
                        TransactionType.TRANSFER_IN,
                        amount,
                        target.balance,
                        LocalDateTime.now(),
                        "Transfer from " + this.accountNumber
                );
                target.transactionHistory.add(txnIn);
            }
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Resets the daily withdrawal counter if the date has changed.
     * Called at the start of withdraw() and transferTo().
     */
    private void resetDailyLimitIfNewDay() {
        LocalDate today = LocalDate.now();
        if (!today.equals(lastWithdrawalDate)) {
            todayWithdrawnTotal = 0;
            lastWithdrawalDate = today;
        }
    }

    /**
     * Adds a transaction directly to the history (used by server for special transactions
     * like interest credits and loan disbursements that bypass deposit/withdraw).
     *
     * @param transaction the transaction to add
     */
    public synchronized void addTransaction(Transaction transaction) {
        this.transactionHistory.add(transaction);
    }

    /**
     * Sets balance directly — used ONLY during file loading (deserialization).
     * Not for regular operations.
     */
    public synchronized void setBalanceFromFile(double balance) {
        this.balance = balance;
    }

    // ==================== SERIALIZATION (for file persistence) ====================

    /**
     * Converts this account to a pipe-delimited string for file storage.
     * Format: accountNumber|holderName|customerId|balance|accountType|dailyLimit|active
     *
     * RUBRIC: Unit 3 — String handling for persistence.
     */
    public String toFileString() {
        StringBuilder sb = new StringBuilder();
        sb.append(accountNumber).append("|");
        sb.append(holderName).append("|");
        sb.append(customerId).append("|");
        sb.append(String.format("%.2f", balance)).append("|");
        sb.append(getAccountType()).append("|");
        sb.append(String.format("%.2f", dailyLimit)).append("|");
        sb.append(active);
        return sb.toString();
    }

    // ==================== GETTERS ====================

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public String getCustomerId() {
        return customerId;
    }

    /**
     * Returns the current balance. Synchronized to ensure we read the
     * most up-to-date value (Java Memory Model visibility guarantee).
     */
    public synchronized double getBalance() {
        return balance;
    }

    /**
     * Returns a COPY of the transaction history to prevent external modification.
     * The caller gets their own list — they can't accidentally add/remove from our internal list.
     */
    public synchronized List<Transaction> getTransactionHistory() {
        return new ArrayList<>(transactionHistory);
    }

    public double getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(double dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public double getTodayWithdrawnTotal() {
        return todayWithdrawnTotal;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return String.format("%s [%s] — %s — Balance: ₹%.2f",
                getAccountType(), accountNumber, holderName, balance);
    }
}
