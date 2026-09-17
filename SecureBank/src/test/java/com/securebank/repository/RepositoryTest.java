package com.securebank.repository;

import com.securebank.core.SavingsAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryTest {

    private final Repository<SavingsAccount> repo = new Repository<>("SavingsAccount");

    private SavingsAccount makeAccount(String id) {
        return new SavingsAccount(id, "Holder-" + id, "C1", 100.0);
    }

    @Test
    @DisplayName("add/get/update/delete round-trip an entity by ID")
    void crudRoundTrip() throws Exception {
        SavingsAccount a = makeAccount("ACC-R-1");
        assertTrue(repo.add("ACC-R-1", a));
        assertEquals(a, repo.get("ACC-R-1"));

        SavingsAccount updated = makeAccount("ACC-R-1");
        updated.deposit(50.0);
        assertTrue(repo.update("ACC-R-1", updated));
        assertEquals(150.0, repo.get("ACC-R-1").getBalance(), 1e-9);

        SavingsAccount deleted = repo.delete("ACC-R-1");
        // delete() returns the currently stored instance — the replacement, not the original
        assertEquals(updated, deleted);
        assertEquals(150.0, deleted.getBalance(), 1e-9);
        assertNull(repo.get("ACC-R-1"));
    }

    @Test
    @DisplayName("add rejects duplicate IDs and update/delete on missing IDs fail cleanly")
    void duplicateAndMissingHandling() {
        assertTrue(repo.add("ACC-R-2", makeAccount("ACC-R-2")));
        assertFalse(repo.add("ACC-R-2", makeAccount("ACC-R-2")));
        assertEquals(1, repo.size());

        assertFalse(repo.update("ACC-NOPE", makeAccount("ACC-NOPE")));
        assertNull(repo.delete("ACC-NOPE"));
    }

    @Test
    @DisplayName("add throws on null ID or null entity")
    void nullArgumentsRejected() {
        assertThrows(IllegalArgumentException.class, () -> repo.add(null, makeAccount("ACC-R-3")));
        assertThrows(IllegalArgumentException.class, () -> repo.add("ACC-R-4", null));
    }

    @Test
    @DisplayName("search applies a lambda predicate and returns matching entities")
    void lambdaSearch() throws Exception {
        repo.add("ACC-R-5", makeAccount("ACC-R-5"));
        SavingsAccount big = makeAccount("ACC-R-6");
        big.deposit(1000.0);
        repo.add("ACC-R-6", big);

        List<SavingsAccount> rich = repo.search(acc -> acc.getBalance() >= 500.0);
        assertEquals(1, rich.size());
        assertEquals("ACC-R-6", rich.get(0).getAccountNumber());
    }

    @Test
    @DisplayName("getAll returns a defensive copy — mutating it does not affect the store")
    void getAllDefensiveCopy() {
        repo.add("ACC-R-7", makeAccount("ACC-R-7"));
        repo.getAll().clear();

        assertEquals(1, repo.size());
    }

    @Test
    @DisplayName("Concurrent add/get from many threads keeps the store consistent")
    void concurrentAccess() throws Exception {
        Repository<SavingsAccount> concurrentRepo = new Repository<>("Concurrent");
        int threads = 8;
        int addsPerThread = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            final int base = t * addsPerThread;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < addsPerThread; i++) {
                        String id = "ACC-C-" + (base + i);
                        concurrentRepo.add(id, makeAccount(id));
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            });
        }

        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));

        assertEquals(0, failures.get());
        assertEquals(threads * addsPerThread, concurrentRepo.size());
    }
}
