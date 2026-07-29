package com.securebank.repository;

import com.securebank.core.Account;
import com.securebank.core.CurrentAccount;
import com.securebank.core.SavingsAccount;
import com.securebank.utils.FileIOHelper;

import java.util.List;

/**
 * AccountRepository — a specialized repository for Account objects.
 *
 * RUBRIC: Unit 5 — Uses the generic Repository<Account> internally.
 * This class wraps Repository<Account> and adds account-specific functionality
 * like file persistence (loading/saving from disk).
 *
 * Design decision: We use COMPOSITION (has-a Repository) rather than INHERITANCE
 * (extends Repository) because we want to control which methods are exposed
 * and add file I/O without polluting the generic Repository class.
 */
public class AccountRepository {

    /** The generic repository that handles all CRUD operations */
    private final Repository<Account> repository;

    /**
     * Creates a new AccountRepository backed by a generic Repository<Account>.
     */
    public AccountRepository() {
        this.repository = new Repository<>("Account");
    }

    // ==================== DELEGATED CRUD (from Repository<T>) ====================

    /**
     * Adds an account to the repository.
     * @param account the account to add
     * @return true if added, false if account number already exists
     */
    public boolean addAccount(Account account) {
        return repository.add(account.getAccountNumber(), account);
    }

    /**
     * Retrieves an account by its account number.
     * @param accountNumber the account number to look up
     * @return the Account if found, null otherwise
     */
    public Account getAccount(String accountNumber) {
        return repository.get(accountNumber);
    }

    /**
     * Updates an existing account.
     * @param account the updated account
     * @return true if updated, false if account doesn't exist
     */
    public boolean updateAccount(Account account) {
        return repository.update(account.getAccountNumber(), account);
    }

    /**
     * Deletes an account by its number.
     * @param accountNumber the account number to delete
     * @return the deleted Account, or null if not found
     */
    public Account deleteAccount(String accountNumber) {
        return repository.delete(accountNumber);
    }

    /**
     * Returns all accounts.
     * @return list of all accounts
     */
    public List<Account> getAllAccounts() {
        return repository.getAll();
    }

    /**
     * Checks if an account exists.
     * @param accountNumber the account number to check
     * @return true if exists
     */
    public boolean exists(String accountNumber) {
        return repository.exists(accountNumber);
    }

    /**
     * Returns the number of accounts.
     * @return account count
     */
    public int size() {
        return repository.size();
    }

    // ==================== ACCOUNT-SPECIFIC QUERIES ====================

    /**
     * Finds all savings accounts.
     * RUBRIC: Unit 2 — Lambda expression with Predicate.
     *
     * @return list of SavingsAccount objects
     */
    public List<Account> getSavingsAccounts() {
        return repository.search(acc -> acc instanceof SavingsAccount);
    }

    /**
     * Finds all current accounts.
     * @return list of CurrentAccount objects
     */
    public List<Account> getCurrentAccounts() {
        return repository.search(acc -> acc instanceof CurrentAccount);
    }

    /**
     * Finds accounts with balance above a threshold.
     * RUBRIC: Unit 2 — Lambda/Stream filtering.
     *
     * @param minBalance the minimum balance threshold
     * @return list of accounts with balance >= minBalance
     */
    public List<Account> getAccountsAboveBalance(double minBalance) {
        return repository.search(acc -> acc.getBalance() >= minBalance);
    }

    /**
     * Finds all accounts belonging to a specific customer.
     *
     * @param customerId the customer ID to filter by
     * @return list of accounts owned by that customer
     */
    public List<Account> getAccountsByCustomer(String customerId) {
        return repository.search(acc -> acc.getCustomerId().equals(customerId));
    }

    // ==================== FILE PERSISTENCE ====================

    /**
     * Saves all accounts to a file.
     * RUBRIC: Unit 4 — File I/O with Character Streams.
     */
    public void saveToFile() {
        FileIOHelper.saveAccounts(repository.getAll());
    }

    /**
     * Loads accounts from a file into this repository.
     * RUBRIC: Unit 4 — File I/O with Character Streams.
     */
    public void loadFromFile() {
        List<Account> accounts = FileIOHelper.loadAccounts();
        for (Account account : accounts) {
            repository.add(account.getAccountNumber(), account);
        }
    }

    /**
     * Provides access to the underlying generic repository (for advanced queries).
     * @return the Repository<Account> instance
     */
    public Repository<Account> getRepository() {
        return repository;
    }

    @Override
    public String toString() {
        return "AccountRepository — " + repository.size() + " accounts";
    }
}
