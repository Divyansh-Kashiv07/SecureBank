package com.securebank.exceptions;

/**
 * InvalidPinException — thrown when a customer provides an incorrect PIN
 * during login or transaction authentication.
 *
 * RUBRIC: Unit 3 — Custom checked exception.
 * This is a checked exception because PIN validation failure is an expected condition
 * that the caller (server/client handler) must handle gracefully (e.g., show error
 * message, allow retry, or lock account after too many attempts).
 */
public class InvalidPinException extends Exception {

    /** The customer ID that attempted authentication */
    private final String customerId;

    /**
     * Constructs the exception with the customer ID that failed authentication.
     *
     * @param customerId the ID of the customer who provided the wrong PIN
     */
    public InvalidPinException(String customerId) {
        super("Invalid PIN provided for customer: " + customerId);
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }
}
