package com.securebank.exceptions;

/**
 * AccountNotFoundException — thrown when an operation references an account number
 * that does not exist in the system.
 *
 * RUBRIC: Unit 3 — Custom checked exception.
 * Common scenarios: fund transfer to non-existent account, balance inquiry
 * with a typo in account number, admin lookup of a deleted account.
 */
public class AccountNotFoundException extends Exception {

    /** The account number that was searched for but not found */
    private final String accountNumber;

    /**
     * Constructs the exception with the missing account number.
     *
     * @param accountNumber the account number that was not found
     */
    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
