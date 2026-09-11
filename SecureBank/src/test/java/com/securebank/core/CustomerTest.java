package com.securebank.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerTest {

    private Customer newCustomer() {
        return new Customer("CUSTOMER-9", "Test Person", "test@securebank.com",
                "9000000000", "Test City", "4321");
    }

    @Test
    @DisplayName("validatePin accepts the correct PIN and rejects wrong/null input")
    void pinValidation() {
        Customer customer = newCustomer();

        assertTrue(customer.validatePin("4321"));
        assertFalse(customer.validatePin("0000"));
        assertFalse(customer.validatePin(null));
        assertFalse(customer.validatePin(""));
    }

    @Test
    @DisplayName("addAccount is idempotent — no duplicate links")
    void addAccountDeduplicates() {
        Customer customer = newCustomer();
        customer.addAccount("ACC-1");
        customer.addAccount("ACC-1");
        customer.addAccount("ACC-2");

        assertEquals(List.of("ACC-1", "ACC-2"), customer.getAccountNumbers());
    }

    @Test
    @DisplayName("removeAccount unlinks the account")
    void removeAccount() {
        Customer customer = newCustomer();
        customer.addAccount("ACC-1");
        customer.removeAccount("ACC-1");

        assertTrue(customer.getAccountNumbers().isEmpty());
    }

    @Test
    @DisplayName("getAccountNumbers returns a defensive copy")
    void accountNumbersDefensiveCopy() {
        Customer customer = newCustomer();
        customer.addAccount("ACC-1");
        customer.getAccountNumbers().clear();

        assertEquals(1, customer.getAccountNumbers().size());
    }

    @Test
    @DisplayName("toFileString/fromFileString round-trips all fields including active flag")
    void fileRoundTrip() {
        Customer original = newCustomer();
        original.addAccount("ACC-1");
        original.addAccount("ACC-2");

        Customer parsed = Customer.fromFileString(original.toFileString());

        assertEquals(original.getCustomerId(), parsed.getCustomerId());
        assertEquals(original.getName(), parsed.getName());
        assertEquals(original.getEmail(), parsed.getEmail());
        assertEquals(original.getPhone(), parsed.getPhone());
        assertEquals(original.getAddress(), parsed.getAddress());
        assertTrue(parsed.validatePin("4321"));
        assertEquals(List.of("ACC-1", "ACC-2"), parsed.getAccountNumbers());
        assertTrue(parsed.isActive());
    }

    @Test
    @DisplayName("Inactive flag survives the file round-trip")
    void inactiveFlagRoundTrip() {
        Customer original = newCustomer();
        original.setActive(false);

        Customer parsed = Customer.fromFileString(original.toFileString());

        assertFalse(parsed.isActive());
    }

    @Test
    @DisplayName("fromFileString returns null for malformed lines")
    void malformedLineReturnsNull() {
        assertNull(Customer.fromFileString("only|five|parts|here|x"));
        assertNull(Customer.fromFileString(""));
    }
}
