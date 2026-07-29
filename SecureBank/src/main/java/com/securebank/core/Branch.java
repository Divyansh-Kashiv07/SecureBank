package com.securebank.core;

/**
 * Branch — represents a physical branch of the cooperative bank.
 *
 * RUBRIC COVERAGE:
 * - Unit 1: Part of the Aggregation relationship (Bank HAS Branches).
 *
 * A Branch is a "part" in the Bank-Branch whole-part relationship, but it has
 * its own independent lifecycle. If the Bank object is destroyed, branches
 * could theoretically continue to exist (unlike Composition).
 *
 * VIVA NOTE — AGGREGATION:
 * Aggregation is a special form of Association where one class represents
 * the "whole" and another represents the "part." The key difference from
 * Composition is that the "part" CAN exist independently of the "whole."
 * Example: A Branch could be transferred to another Bank.
 */
public class Branch {

    /** Unique branch code (e.g., "BR-001") */
    private final String branchCode;

    /** Branch name (e.g., "Greater Noida Main Branch") */
    private String name;

    /** Physical address of the branch */
    private String address;

    /** IFSC code for the branch (used in fund transfers) */
    private String ifscCode;

    /** Branch manager name */
    private String managerName;

    /** Contact phone number */
    private String phone;

    /**
     * Creates a new Branch.
     *
     * @param branchCode unique branch identifier
     * @param name       branch name
     * @param address    physical address
     * @param ifscCode   IFSC code for fund transfers
     */
    public Branch(String branchCode, String name, String address, String ifscCode) {
        this.branchCode = branchCode;
        this.name = name;
        this.address = address;
        this.ifscCode = ifscCode;
        this.managerName = "";
        this.phone = "";
    }

    // ==================== GETTERS & SETTERS ====================

    public String getBranchCode() {
        return branchCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return String.format("Branch [%s] — %s (%s)", branchCode, name, ifscCode);
    }
}
