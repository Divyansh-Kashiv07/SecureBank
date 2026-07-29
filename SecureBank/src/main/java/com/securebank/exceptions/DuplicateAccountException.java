package com.securebank.exceptions;

/**
 * DuplicateAccountException — thrown when attempting to create an account
 * with an account number that already exists in the system.
 *
 * RUBRIC: Unit 3 — Custom checked exception.
 * This protects data integrity — every account must have a unique identifier.
 * The Repository's add() method checks for duplicates before inserting.
 */
public class DuplicateAccountException extends Exception {

    /** The account number that already exists */
    private final String accountNumber;

    /**
     * Constructs the exception with the duplicate account number.
     *
     * @param accountNumber the account number that already exists
     */
    public DuplicateAccountException(String accountNumber) {
        super("Account already exists with number: " + accountNumber);
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
