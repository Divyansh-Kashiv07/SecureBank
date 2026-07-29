package com.securebank.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer — represents a bank customer who can hold one or more accounts.
 *
 * RUBRIC COVERAGE:
 * - Unit 1: Association with Account — a Customer HAS accounts, but both
 *   can exist independently. If a Customer is deleted, their Account objects
 *   could theoretically still exist (unlike Composition where children die with parent).
 *
 * VIVA NOTE — ASSOCIATION vs COMPOSITION vs AGGREGATION:
 * - Association (Customer–Account): "Customer HAS accounts" — loose coupling,
 *   both can exist independently.
 * - Composition (Account–Transaction): "Account OWNS transactions" — if Account
 *   is deleted, its Transactions are destroyed too. Strong ownership.
 * - Aggregation (Bank–Branch): "Bank HAS branches" — branches could exist
 *   independently (e.g., be transferred to another bank). Whole-part relationship
 *   but the part has its own lifecycle.
 */
public class Customer {

    /** Unique customer identifier (e.g., "CUSTOMER-1") */
    private final String customerId;

    /** Customer's full name */
    private String name;

    /** Customer's email address */
    private String email;

    /** Customer's phone number */
    private String phone;

    /** Customer's residential address */
    private String address;

    /** 4-digit PIN for authentication (stored as hash in production; plain for academic demo) */
    private String pin;

    /**
     * ASSOCIATION: List of account numbers belonging to this customer.
     * We store account NUMBERS (not Account objects) to avoid tight coupling.
     * The actual Account objects live in the AccountRepository.
     */
    private final List<String> accountNumbers;

    /** Whether this customer account is active */
    private boolean active;

    /**
     * Full constructor — creates a customer with all details.
     *
     * @param customerId unique customer identifier
     * @param name       full name
     * @param email      email address
     * @param phone      phone number
     * @param address    residential address
     * @param pin        4-digit authentication PIN
     */
    public Customer(String customerId, String name, String email, String phone,
                    String address, String pin) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.pin = pin;
        this.accountNumbers = new ArrayList<>();
        this.active = true;
    }

    /**
     * Validates the provided PIN against the stored PIN.
     *
     * @param inputPin the PIN to validate
     * @return true if the PIN matches, false otherwise
     */
    public boolean validatePin(String inputPin) {
        return this.pin != null && this.pin.equals(inputPin);
    }

    /**
     * Links an account to this customer (Association).
     *
     * @param accountNumber the account number to add
     */
    public void addAccount(String accountNumber) {
        if (!accountNumbers.contains(accountNumber)) {
            accountNumbers.add(accountNumber);
        }
    }

    /**
     * Removes an account link from this customer.
     *
     * @param accountNumber the account number to remove
     */
    public void removeAccount(String accountNumber) {
        accountNumbers.remove(accountNumber);
    }

    // ==================== SERIALIZATION ====================

    /**
     * Converts this customer to a pipe-delimited string for file storage.
     * Format: customerId|name|email|phone|address|pin|accountNumbers(comma-separated)|active
     */
    public String toFileString() {
        StringBuilder sb = new StringBuilder();
        sb.append(customerId).append("|");
        sb.append(name).append("|");
        sb.append(email).append("|");
        sb.append(phone).append("|");
        sb.append(address).append("|");
        sb.append(pin).append("|");
        sb.append(String.join(",", accountNumbers)).append("|");
        sb.append(active);
        return sb.toString();
    }

    /**
     * Parses a pipe-delimited string back into a Customer object.
     *
     * @param line the pipe-delimited string from the data file
     * @return a new Customer object, or null if parsing fails
     */
    public static Customer fromFileString(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 7) return null;

            Customer customer = new Customer(
                    parts[0],   // customerId
                    parts[1],   // name
                    parts[2],   // email
                    parts[3],   // phone
                    parts[4],   // address
                    parts[5]    // pin
            );

            // Parse account numbers (comma-separated)
            String accountsStr = parts[6];
            if (accountsStr != null && !accountsStr.trim().isEmpty()) {
                String[] accounts = accountsStr.split(",");
                for (String acc : accounts) {
                    if (!acc.trim().isEmpty()) {
                        customer.addAccount(acc.trim());
                    }
                }
            }

            // Parse active status
            if (parts.length > 7) {
                customer.active = Boolean.parseBoolean(parts[7]);
            }

            return customer;
        } catch (Exception e) {
            System.err.println("Warning: Could not parse customer line: " + line);
            return null;
        }
    }

    // ==================== GETTERS & SETTERS ====================

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    /**
     * Returns a COPY of the account numbers list to prevent external modification.
     */
    public List<String> getAccountNumbers() {
        return new ArrayList<>(accountNumbers);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return String.format("Customer [%s] — %s (%s) — %d account(s)",
                customerId, name, email, accountNumbers.size());
    }
}
