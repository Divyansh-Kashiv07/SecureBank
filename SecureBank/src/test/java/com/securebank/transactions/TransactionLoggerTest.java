package com.securebank.transactions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 4 reliability: TransactionLogger.stop() must DRAIN the queue — every
 * transaction logged before shutdown is written to the file, never dropped.
 */
class TransactionLoggerTest {

    @TempDir
    Path tempDir;

    @Test
    @Timeout(30)
    @DisplayName("stop() drains all queued transactions to the log file")
    void stopDrainsQueue() throws Exception {
        Path logFile = tempDir.resolve("txn_log.txt");

        TransactionLogger logger = new TransactionLogger(logFile.toString());
        logger.start();

        for (int i = 1; i <= 25; i++) {
            logger.log(new Transaction(
                    "TXN-DR-" + i, "ACC-DRAIN", TransactionType.DEPOSIT,
                    i * 10.0, i * 10.0, LocalDateTime.now(), "drain test " + i));
        }

        // Stop immediately — before the daemon necessarily processed everything
        logger.stop();

        // The drained file must contain ALL 25 transactions
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(logFile)
                    && Files.readAllLines(logFile).stream()
                        .filter(l -> l.contains("TXN: TXN-DR-")).count() >= 25) {
                break;
            }
            Thread.sleep(50);
        }

        assertTrue(Files.exists(logFile), "log file must exist after drain");
        long written = Files.readAllLines(logFile).stream()
                .filter(l -> l.contains("TXN: TXN-DR-")).count();
        assertTrue(written >= 25,
                "all 25 logged transactions must be written before shutdown (got " + written + ")");
    }
}
