package com.securebank.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency tests at the PROTOCOL level: multiple real clients hammering the
 * same account and opposite-direction transfers through the server.
 *
 * These are the tests that would catch a lost-update race or a transfer deadlock
 * in the multi-client server path, not just in the domain classes.
 */
class ConcurrencyIntegrationTest extends ServerHarness {

    private static final int THREADS = 8;
    private static final int OPS_PER_THREAD = 10;

    @Test
    @Timeout(60)
    @DisplayName("Concurrent deposits from many clients all land — no lost updates")
    void concurrentDepositsAreAllRecorded() throws Exception {
        ProtocolClient setup = newClient();
        setup.sendOk("LOGIN|CUSTOMER-3|9012");
        // Seed a fresh account directly through the protocol
        String accountNumber = setup.sendOk("CREATE_ACCOUNT|Savings|0");
        setup.sendOk("DEPOSIT|" + accountNumber + "|1000.0");
        double before = Double.parseDouble(setup.sendOk("BALANCE|" + accountNumber));

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        AtomicInteger failures = new AtomicInteger();
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (int t = 0; t < THREADS; t++) {
                futures.add(pool.submit((Callable<Boolean>) () -> {
                    try (ProtocolClient c = newClient()) {
                        c.sendOk("LOGIN|CUSTOMER-3|9012");
                        for (int i = 0; i < OPS_PER_THREAD; i++) {
                            String resp = c.send("DEPOSIT|" + accountNumber + "|100");
                            if (!resp.startsWith("OK|")) {
                                failures.incrementAndGet();
                                return false;
                            }
                        }
                        return true;
                    } catch (IOException e) {
                        failures.incrementAndGet();
                        return false;
                    }
                }));
            }
            for (Future<Boolean> f : futures) {
                assertTrue(f.get(45, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(0, failures.get());
        double after = Double.parseDouble(setup.sendOk("BALANCE|" + accountNumber));
        assertEquals(before + THREADS * OPS_PER_THREAD * 100.0, after, 1e-9,
                "Every concurrent deposit must be recorded — a mismatch means a lost update");

        setup.close();
    }

    @Test
    @Timeout(90)
    @DisplayName("Concurrent opposite-direction transfers through the server never deadlock")
    void concurrentOppositeTransfers() throws Exception {
        ProtocolClient setup = newClient();
        setup.sendOk("LOGIN|CUSTOMER-3|9012");
        String accA = setup.sendOk("CREATE_ACCOUNT|Savings|5000");
        String accB = setup.sendOk("CREATE_ACCOUNT|Savings|5000");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> aToB = pool.submit((Callable<Boolean>) () -> {
                try (ProtocolClient c = newClient()) {
                    c.sendOk("LOGIN|CUSTOMER-3|9012");
                    for (int i = 0; i < 25; i++) {
                        if (!c.send("TRANSFER|" + accA + "|" + accB + "|10").startsWith("OK|")) {
                            return false;
                        }
                    }
                    return true;
                }
            });
            Future<Boolean> bToA = pool.submit((Callable<Boolean>) () -> {
                try (ProtocolClient c = newClient()) {
                    c.sendOk("LOGIN|CUSTOMER-3|9012");
                    for (int i = 0; i < 25; i++) {
                        if (!c.send("TRANSFER|" + accB + "|" + accA + "|10").startsWith("OK|")) {
                            return false;
                        }
                    }
                    return true;
                }
            });

            assertTrue(aToB.get(60, TimeUnit.SECONDS), "A→B transfers must all succeed");
            assertTrue(bToA.get(60, TimeUnit.SECONDS), "B→A transfers must all succeed");
        } finally {
            pool.shutdownNow();
        }

        double totalAfter = Double.parseDouble(setup.sendOk("BALANCE|" + accA))
                + Double.parseDouble(setup.sendOk("BALANCE|" + accB));
        assertEquals(10000.0, totalAfter, 1e-9, "Total funds must be conserved");

        setup.close();
    }

    @Test
    @Timeout(60)
    @DisplayName("Concurrent withdrawals never breach the savings minimum-balance floor")
    void concurrentWithdrawalsNeverOverdraw() throws Exception {
        ProtocolClient setup = newClient();
        setup.sendOk("LOGIN|CUSTOMER-3|9012");
        // 1500 balance, 1000 minimum → at most 5 withdrawals of 100 may succeed,
        // no matter how the concurrent threads interleave
        String acc = setup.sendOk("CREATE_ACCOUNT|Savings|1500");

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        AtomicInteger successCount = new AtomicInteger();
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (int t = 0; t < THREADS; t++) {
                futures.add(pool.submit((Callable<Boolean>) () -> {
                    try (ProtocolClient c = newClient()) {
                        c.sendOk("LOGIN|CUSTOMER-3|9012");
                        for (int i = 0; i < OPS_PER_THREAD; i++) {
                            String resp = c.send("WITHDRAW|" + acc + "|100");
                            if (resp.startsWith("OK|")) {
                                successCount.incrementAndGet();
                            } else if (!resp.startsWith("ERROR|")) {
                                return false; // neither OK nor ERROR is a protocol violation
                            }
                        }
                        return true;
                    }
                }));
            }
            for (Future<Boolean> f : futures) {
                assertTrue(f.get(45, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        // Balance is 1500, minimum is 1000 → at most 5 withdrawals of 100 can succeed
        assertTrue(successCount.get() <= 5,
                "More successful withdrawals than the balance allows means a race condition");
        double finalBalance = Double.parseDouble(setup.sendOk("BALANCE|" + acc));
        assertTrue(finalBalance >= 1000.0,
                "Savings minimum-balance floor must hold even under concurrent withdrawals");
        assertEquals(1500.0 - successCount.get() * 100.0, finalBalance, 1e-9);

        setup.close();
    }
}
