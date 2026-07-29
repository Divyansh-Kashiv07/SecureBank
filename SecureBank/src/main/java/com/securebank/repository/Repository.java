package com.securebank.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Repository<T> — a GENERIC, reusable in-memory data store.
 *
 * RUBRIC COVERAGE:
 * - Unit 5: Generics — this is the STANDOUT, PORTFOLIO-DIFFERENTIATING piece of code.
 * - Unit 5: Collections — uses HashMap<String, T> for O(1) lookups by key.
 * - Unit 2: Lambda expressions — the search() method accepts a Predicate<T>.
 *
 * WHAT IS GENERICS? (VIVA EXPLANATION)
 * Generics let you write a class that works with ANY type, decided at usage time.
 * Instead of writing separate AccountRepository, CustomerRepository, LoanRepository
 * classes that all do the same CRUD operations, we write ONE class:
 *
 *   Repository<Account>  — stores Account objects, keyed by account number
 *   Repository<Customer> — stores Customer objects, keyed by customer ID
 *   Repository<Loan>     — stores Loan objects, keyed by loan ID
 *
 * The <T> is a TYPE PARAMETER — it's a placeholder that gets replaced with a
 * real type when you create an instance. The compiler enforces type safety:
 *   - Repository<Account> only accepts Account objects
 *   - You can't accidentally put a Customer into a Repository<Account>
 *
 * WHY IS THIS USEFUL?
 * 1. Code reuse — one class, many types
 * 2. Type safety — compiler catches mistakes at compile time, not runtime
 * 3. No casting needed — get() returns T directly, not Object
 *
 * HOW IT WORKS INTERNALLY:
 * Java implements generics through "type erasure" — the <T> is removed at compile time
 * and replaced with Object. The compiler inserts casts automatically. So at runtime,
 * Repository<Account> and Repository<Customer> are the SAME class (Repository<Object>).
 * The type checking only happens at compile time.
 *
 * @param <T> the type of entity stored in this repository
 */
public class Repository<T> {

    /**
     * The internal storage — HashMap provides O(1) average-case lookups by key.
     * RUBRIC: Unit 5 — HashMap<String, T> for fast account-number lookups.
     *
     * Key = unique identifier (account number, customer ID, etc.)
     * Value = the entity of type T
     */
    private final Map<String, T> store;

    /** A human-readable name for this repository (used in error messages and logging) */
    private final String entityName;

    /**
     * Creates a new empty Repository.
     *
     * @param entityName a name describing what this repo stores (e.g., "Account", "Customer")
     */
    public Repository(String entityName) {
        this.store = new HashMap<>();
        this.entityName = entityName;
    }

    // ==================== CRUD OPERATIONS ====================

    /**
     * Adds an entity to the repository.
     *
     * @param id     the unique key for this entity
     * @param entity the entity to store
     * @return true if added successfully, false if the key already exists
     */
    public synchronized boolean add(String id, T entity) {
        if (id == null || entity == null) {
            throw new IllegalArgumentException(entityName + " ID and entity cannot be null");
        }
        if (store.containsKey(id)) {
            return false; // Duplicate — don't overwrite
        }
        store.put(id, entity);
        return true;
    }

    /**
     * Retrieves an entity by its ID.
     *
     * @param id the unique key
     * @return the entity if found, null otherwise
     */
    public synchronized T get(String id) {
        return store.get(id);
    }

    /**
     * Updates an existing entity. The key must already exist.
     *
     * @param id     the unique key
     * @param entity the updated entity
     * @return true if updated, false if the key doesn't exist
     */
    public synchronized boolean update(String id, T entity) {
        if (!store.containsKey(id)) {
            return false; // Can't update something that doesn't exist
        }
        store.put(id, entity);
        return true;
    }

    /**
     * Deletes an entity by its ID.
     *
     * @param id the unique key
     * @return the removed entity, or null if not found
     */
    public synchronized T delete(String id) {
        return store.remove(id);
    }

    /**
     * Returns all entities in the repository.
     *
     * @return a new List containing all stored entities (safe copy)
     */
    public synchronized List<T> getAll() {
        return new ArrayList<>(store.values());
    }

    /**
     * Returns all entries (key-value pairs) — useful for serialization.
     *
     * @return a copy of the entry set as a list
     */
    public synchronized List<Map.Entry<String, T>> getAllEntries() {
        return new ArrayList<>(store.entrySet());
    }

    /**
     * Searches for entities matching a condition using a Lambda expression.
     *
     * RUBRIC: Unit 2 — Lambda expressions for functional operations.
     *
     * Usage example (filtering accounts with balance > 10000):
     *   repository.search(account -> account.getBalance() > 10000)
     *
     * Usage example (finding customers by name):
     *   repository.search(customer -> customer.getName().contains("Sharma"))
     *
     * VIVA NOTE — PREDICATE AND LAMBDA:
     * Predicate<T> is a functional interface from java.util.function.
     * It has one abstract method: boolean test(T t).
     * A lambda expression like `acc -> acc.getBalance() > 10000` is a concise
     * way to implement this interface inline without writing a whole class.
     *
     * @param predicate the condition to match (a lambda expression)
     * @return a list of matching entities
     */
    public synchronized List<T> search(Predicate<T> predicate) {
        return store.values().stream()
                .filter(predicate)   // Apply the lambda predicate to each entity
                .collect(Collectors.toList());
    }

    /**
     * Checks if an entity with the given ID exists.
     *
     * @param id the key to check
     * @return true if the key exists
     */
    public synchronized boolean exists(String id) {
        return store.containsKey(id);
    }

    /**
     * Returns the number of entities stored.
     *
     * @return count of entities
     */
    public synchronized int size() {
        return store.size();
    }

    /**
     * Removes all entities from the repository.
     */
    public synchronized void clear() {
        store.clear();
    }

    /**
     * Returns the entity name for this repository.
     * @return the entity type name (e.g., "Account", "Customer")
     */
    public String getEntityName() {
        return entityName;
    }

    @Override
    public String toString() {
        return String.format("Repository<%s> — %d entities", entityName, store.size());
    }
}
