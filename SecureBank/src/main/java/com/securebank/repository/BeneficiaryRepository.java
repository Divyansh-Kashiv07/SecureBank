package com.securebank.repository;

import com.securebank.core.Beneficiary;
import com.securebank.utils.FileIOHelper;

import java.io.IOException;
import java.util.List;

/**
 * BeneficiaryRepository — a specialized repository for Beneficiary objects.
 *
 * Same pattern as AccountRepository and CustomerRepository: it wraps the
 * generic Repository<Beneficiary> for CRUD + type-safe lookups and adds the
 * beneficiary-specific query used by the service layer (list all payees of
 * one customer) plus file persistence.
 */
public class BeneficiaryRepository {

    /** The generic repository that handles all CRUD operations */
    private final Repository<Beneficiary> repository;

    public BeneficiaryRepository() {
        this.repository = new Repository<>("Beneficiary");
    }

    // ==================== DELEGATED CRUD ====================

    public boolean addBeneficiary(Beneficiary beneficiary) {
        return repository.add(beneficiary.getBeneficiaryId(), beneficiary);
    }

    public Beneficiary getBeneficiary(String beneficiaryId) {
        return repository.get(beneficiaryId);
    }

    public Beneficiary deleteBeneficiary(String beneficiaryId) {
        return repository.delete(beneficiaryId);
    }

    public int size() {
        return repository.size();
    }

    public List<Beneficiary> getAll() {
        return repository.getAll();
    }

    // ==================== BENEFICIARY-SPECIFIC QUERIES ====================

    /**
     * Returns every payee saved by one customer.
     *
     * @param customerId the owning customer
     * @return the customer's beneficiaries (possibly empty, never null)
     */
    public List<Beneficiary> getByCustomer(String customerId) {
        return repository.search(b -> b.getCustomerId().equals(customerId));
    }

    /**
     * Returns true if the customer already saved this account number as a payee,
     * so duplicate payees can be rejected.
     */
    public boolean isDuplicate(String customerId, String accountNumber) {
        return !repository.search(b -> b.getCustomerId().equals(customerId)
                && b.getAccountNumber().equalsIgnoreCase(accountNumber)).isEmpty();
    }

    // ==================== FILE PERSISTENCE ====================

    /**
     * Saves all beneficiaries to disk.
     *
     * @throws IOException if persistence fails — callers must handle this
     */
    public void saveToFile() throws IOException {
        FileIOHelper.saveBeneficiaries(repository.getAll());
    }

    /**
     * Loads beneficiaries from disk.
     */
    public void loadFromFile() {
        for (Beneficiary beneficiary : FileIOHelper.loadBeneficiaries()) {
            repository.add(beneficiary.getBeneficiaryId(), beneficiary);
        }
    }

    @Override
    public String toString() {
        return "BeneficiaryRepository — " + repository.size() + " beneficiaries";
    }
}
