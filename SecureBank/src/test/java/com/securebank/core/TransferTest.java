package com.securebank.core;

import com.securebank.exceptions.DailyLimitExceededException;
import com.securebank.exceptions.InsufficientBalanceException;
import com.securebank.transactions.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferTest {

    private SavingsAccount source;
    private CurrentAccount target;

    @BeforeEach
    void setUp() {
        // Seed above the savings minimum balance (₹1,000) so outgoing transfers are allowed
        source = new SavingsAccount("ACC-T-SRC", "Source", "C1", 2000.0);
        target = new CurrentAccount("ACC-T-DST", "Target", "C2", 1000.0);
    }

    @Test
    @DisplayName("Transfer moves funds and records TRANSFER_OUT/TRANSFER_IN on both sides")
    void transferMovesFunds() throws Exception {
        source.transferTo(target, 400.0);

        assertEquals(1600.0, source.getBalance(), 1e-9);
        assertEquals(1400.0, target.getBalance(), 1e-9);

        assertEquals(1, source.getTransactionHistory().size());
        assertEquals(TransactionType.TRANSFER_OUT,
                source.getTransactionHistory().get(0).getType());
        assertEquals(1, target.getTransactionHistory().size());
        assertEquals(TransactionType.TRANSFER_IN,
                target.getTransactionHistory().get(0).getType());
    }

    @Test
    @DisplayName("Transfer that would break the balance rule throws and leaves both sides unchanged")
    void transferBeyondBalanceThrows() {
        // Seed is 2000, savings minimum is 1000 → max transferable is 1000.00
        assertThrows(InsufficientBalanceException.class,
                () -> source.transferTo(target, 1000.01));

        assertEquals(2000.0, source.getBalance(), 1e-9);
        assertEquals(1000.0, target.getBalance(), 1e-9);
        assertTrue(source.getTransactionHistory().isEmpty());
        assertTrue(target.getTransactionHistory().isEmpty());
    }

    @Test
    @DisplayName("Transfer respects the source daily limit and leaves both sides unchanged")
    void transferRespectsDailyLimit() {
        source.setDailyLimit(100.0);

        assertThrows(DailyLimitExceededException.class,
                () -> source.transferTo(target, 150.0));

        assertEquals(2000.0, source.getBalance(), 1e-9);
        assertEquals(1000.0, target.getBalance(), 1e-9);
    }

    @Test
    @DisplayName("Self-transfer, null target and non-positive amounts are rejected")
    void transferValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> source.transferTo(source, 100.0));
        assertThrows(IllegalArgumentException.class,
                () -> source.transferTo(null, 100.0));
        assertThrows(IllegalArgumentException.class,
                () -> source.transferTo(target, 0));
        assertThrows(IllegalArgumentException.class,
                () -> source.transferTo(target, -50));
    }

    @Test
    @Timeout(15)
    @DisplayName("Concurrent opposite-direction transfers never deadlock and conserve the total")
    void concurrentOppositeTransfersAreDeadlockFreeAndConserveTotal() throws Exception {
        // Seeds stay above the savings minimum balance after 50 outgoing transfers
        SavingsAccount a = new SavingsAccount("ACC-DL-A", "A", "C1", 5000.0);
        CurrentAccount b = new CurrentAccount("ACC-DL-B", "B", "C2", 5000.0);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> aToB = pool.submit(() -> {
                for (int i = 0; i < 50; i++) {
                    try {
                        a.transferTo(b, 1.0);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            Future<?> bToA = pool.submit(() -> {
                for (int i = 0; i < 50; i++) {
                    try {
                        b.transferTo(a, 1.0);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            // A deadlock would surface here as a TimeoutException
            aToB.get(10, TimeUnit.SECONDS);
            bToA.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertEquals(5000.0, a.getBalance(), 1e-9);
        assertEquals(5000.0, b.getBalance(), 1e-9);
        assertEquals(10000.0, a.getBalance() + b.getBalance(), 1e-9);
    }
}
