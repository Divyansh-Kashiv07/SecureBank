package com.securebank.exceptions;

/**
 * AccountInactiveException — thrown when an operation is attempted on an account
 * that has been frozen/deactivated (active == false).
 *
 * RUBRIC: Unit 3 — Custom checked exception. Frozen accounts are a legitimate
 * business state, so callers must handle this explicitly rather than treating
 * it as a programming error.
 */
public class AccountInactiveException extends Exception {

    /** The account number that is inactive */
    private final String accountNumber;

    /**
     * Constructs the exception for the given account.
     *
     * @param accountNumber the inactive account's number
     */
    public AccountInactiveException(String accountNumber) {
        super("Account is inactive (frozen): " + accountNumber
                + ". Contact the bank to reactivate it.");
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
