package com.securebank.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Bank — the top-level entity representing the cooperative bank itself.
 *
 * RUBRIC COVERAGE:
 * - Unit 1: Aggregation (Bank HAS Branches — whole-part relationship).
 *
 * This class represents "SecureBank Cooperative" as an entity. It aggregates
 * (contains) multiple Branch objects. The relationship is Aggregation, not
 * Composition, because branches have their own lifecycle — they could be
 * transferred to another bank or exist independently.
 *
 * VIVA NOTE — AGGREGATION vs COMPOSITION:
 * - Aggregation (Bank–Branch): The Bank HAS branches. If we delete the Bank
 *   object from memory, the Branch objects could still be meaningful on their own.
 *   In code: Bank stores a List<Branch>, but doesn't create/destroy them internally.
 *
 * - Composition (Account–Transaction): The Account OWNS its transactions.
 *   Transactions are created INSIDE Account methods (deposit/withdraw) and have
 *   no meaning without their parent Account.
 *
 * The simplest way to spot the difference: In Composition, the parent CREATES
 * the child. In Aggregation, the child is added from outside.
 */
public class Bank {

    /** Name of the cooperative bank */
    private final String bankName;

    /** Unique bank code */
    private final String bankCode;

    /**
     * AGGREGATION: Bank HAS branches.
     * Branches are added from outside (addBranch), not created internally.
     * This is the key indicator of Aggregation.
     */
    private final List<Branch> branches;

    /** Head office address */
    private String headOffice;

    /**
     * Creates a new Bank.
     *
     * @param bankName the name of the cooperative bank
     * @param bankCode unique bank code
     * @param headOffice address of the head office
     */
    public Bank(String bankName, String bankCode, String headOffice) {
        this.bankName = bankName;
        this.bankCode = bankCode;
        this.headOffice = headOffice;
        this.branches = new ArrayList<>();
    }

    /**
     * Adds a branch to this bank (Aggregation).
     * The Branch object was created OUTSIDE the Bank — we're just storing a reference.
     * This is what makes it Aggregation, not Composition.
     *
     * @param branch the branch to add
     */
    public void addBranch(Branch branch) {
        if (branch != null && !branches.contains(branch)) {
            branches.add(branch);
        }
    }

    /**
     * Removes a branch from this bank.
     * Note: The Branch object still exists after removal — it's not destroyed.
     * This is Aggregation behavior.
     *
     * @param branchCode the code of the branch to remove
     * @return true if the branch was found and removed
     */
    public boolean removeBranch(String branchCode) {
        return branches.removeIf(b -> b.getBranchCode().equals(branchCode));
    }

    /**
     * Finds a branch by its code.
     *
     * @param branchCode the branch code to search for
     * @return the Branch if found, null otherwise
     */
    public Branch findBranch(String branchCode) {
        for (Branch branch : branches) {
            if (branch.getBranchCode().equals(branchCode)) {
                return branch;
            }
        }
        return null;
    }

    /**
     * Returns the total number of branches.
     * @return branch count
     */
    public int getBranchCount() {
        return branches.size();
    }

    // ==================== GETTERS ====================

    public String getBankName() {
        return bankName;
    }

    public String getBankCode() {
        return bankCode;
    }

    /**
     * Returns a copy of the branches list.
     */
    public List<Branch> getBranches() {
        return new ArrayList<>(branches);
    }

    public String getHeadOffice() {
        return headOffice;
    }

    public void setHeadOffice(String headOffice) {
        this.headOffice = headOffice;
    }

    @Override
    public String toString() {
        return String.format("Bank: %s [%s] — %d branch(es)", bankName, bankCode, branches.size());
    }
}
