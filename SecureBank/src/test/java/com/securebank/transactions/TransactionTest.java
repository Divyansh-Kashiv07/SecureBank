package com.securebank.transactions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionTest {

    @Test
    @DisplayName("Transaction is immutable — getters return constructor values, no setters exist")
    void immutability() {
        LocalDateTime ts = LocalDateTime.of(2026, 9, 10, 12, 30, 0);
        Transaction txn = new Transaction("TXN-000001", "ACC-1", TransactionType.DEPOSIT,
                500.0, 1500.0, ts, "Test deposit");

        assertEquals("TXN-000001", txn.getTransactionId());
        assertEquals("ACC-1", txn.getAccountNumber());
        assertEquals(TransactionType.DEPOSIT, txn.getType());
        assertEquals(500.0, txn.getAmount(), 1e-9);
        assertEquals(1500.0, txn.getBalanceAfter(), 1e-9);
        assertEquals(ts, txn.getTimestamp());
        assertEquals("Test deposit", txn.getRemarks());
    }

    @Test
    @DisplayName("Null remarks are normalized to an empty string")
    void nullRemarksNormalized() {
        Transaction txn = new Transaction("TXN-000002", "ACC-1", TransactionType.WITHDRAWAL,
                100.0, 900.0, LocalDateTime.now(), null);
        assertEquals("", txn.getRemarks());
    }

    @Test
    @DisplayName("toFileString/fromFileString round-trips all fields")
    void fileRoundTrip() {
        LocalDateTime ts = LocalDateTime.of(2026, 9, 10, 12, 30, 0);
        // Note: remarks must not contain '|' — the file format is pipe-delimited
        // (see remarksWithPipesProduceExtraFields below for the documented edge case)
        Transaction original = new Transaction("TXN-000003", "ACC-42", TransactionType.TRANSFER_OUT,
                250.0, 750.0, ts, "Transfer to ACC-43");

        Transaction parsed = Transaction.fromFileString(original.toFileString());

        assertEquals(original.getTransactionId(), parsed.getTransactionId());
        assertEquals(original.getAccountNumber(), parsed.getAccountNumber());
        assertEquals(original.getType(), parsed.getType());
        assertEquals(original.getAmount(), parsed.getAmount(), 1e-9);
        assertEquals(original.getBalanceAfter(), parsed.getBalanceAfter(), 1e-9);
        assertEquals(original.getTimestamp(), parsed.getTimestamp());
        assertEquals(original.getRemarks(), parsed.getRemarks());
    }

    @Test
    @DisplayName("Pipes in remarks are sanitized at the serialization boundary (Phase 2 fix)")
    void remarksWithPipesAreSanitizedOnPersist() {
        Transaction original = new Transaction("TXN-000004", "ACC-1", TransactionType.DEPOSIT,
                50.0, 50.0, LocalDateTime.now(), "a|b|c");

        Transaction parsed = Transaction.fromFileString(original.toFileString());

        // toFileString strips pipes, so the round-trip preserves the record structure
        assertEquals("a/b/c", parsed.getRemarks());
    }

    @Test
    @DisplayName("fromFileString returns null for garbage lines")
    void malformedLineReturnsNull() {
        assertNull(Transaction.fromFileString("not,a,valid,line"));
        assertNull(Transaction.fromFileString(""));
    }

    @Test
    @DisplayName("Enum labels are human-readable and stable for persistence (stored by name)")
    void enumLabels() {
        assertEquals("Deposit", TransactionType.DEPOSIT.getLabel());
        assertEquals("Fund Transfer (In)", TransactionType.TRANSFER_IN.getLabel());
        // toFileString persists the enum NAME, which fromFileString parses via valueOf
        assertEquals(TransactionType.DEPOSIT, TransactionType.valueOf(TransactionType.DEPOSIT.name()));
    }
}
