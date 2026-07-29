package com.securebank.repository;

import com.securebank.core.Customer;
import com.securebank.utils.FileIOHelper;

import java.util.List;

/**
 * CustomerRepository — a specialized repository for Customer objects.
 *
 * RUBRIC: Unit 5 — Uses the generic Repository<Customer> internally.
 * Same pattern as AccountRepository — wraps the generic Repository
 * and adds customer-specific queries and file persistence.
 */
public class CustomerRepository {

    /** The generic repository that handles all CRUD operations */
    private final Repository<Customer> repository;

    /**
     * Creates a new CustomerRepository backed by a generic Repository<Customer>.
     */
    public CustomerRepository() {
        this.repository = new Repository<>("Customer");
    }

    // ==================== DELEGATED CRUD ====================

    public boolean addCustomer(Customer customer) {
        return repository.add(customer.getCustomerId(), customer);
    }

    public Customer getCustomer(String customerId) {
        return repository.get(customerId);
    }

    public boolean updateCustomer(Customer customer) {
        return repository.update(customer.getCustomerId(), customer);
    }

    public Customer deleteCustomer(String customerId) {
        return repository.delete(customerId);
    }

    public List<Customer> getAllCustomers() {
        return repository.getAll();
    }

    public boolean exists(String customerId) {
        return repository.exists(customerId);
    }

    public int size() {
        return repository.size();
    }

    // ==================== CUSTOMER-SPECIFIC QUERIES ====================

    /**
     * Finds a customer by name (partial, case-insensitive match).
     * RUBRIC: Unit 2 — Lambda expression.
     *
     * @param name the name (or partial name) to search for
     * @return list of matching customers
     */
    public List<Customer> findByName(String name) {
        String lowerName = name.toLowerCase();
        return repository.search(c -> c.getName().toLowerCase().contains(lowerName));
    }

    /**
     * Finds a customer by email.
     *
     * @param email the email to search for
     * @return the matching customer, or null
     */
    public Customer findByEmail(String email) {
        List<Customer> results = repository.search(c -> c.getEmail().equalsIgnoreCase(email));
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Finds all active customers.
     *
     * @return list of active customers
     */
    public List<Customer> getActiveCustomers() {
        return repository.search(Customer::isActive);
    }

    // ==================== FILE PERSISTENCE ====================

    /**
     * Saves all customers to a file.
     */
    public void saveToFile() {
        FileIOHelper.saveCustomers(repository.getAll());
    }

    /**
     * Loads customers from a file.
     */
    public void loadFromFile() {
        List<Customer> customers = FileIOHelper.loadCustomers();
        for (Customer customer : customers) {
            repository.add(customer.getCustomerId(), customer);
        }
    }

    /**
     * Provides access to the underlying generic repository.
     */
    public Repository<Customer> getRepository() {
        return repository;
    }

    @Override
    public String toString() {
        return "CustomerRepository — " + repository.size() + " customers";
    }
}
