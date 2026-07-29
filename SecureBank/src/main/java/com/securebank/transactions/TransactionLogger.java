package com.securebank.transactions;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * TransactionLogger — a DAEMON THREAD that asynchronously logs transactions to a file.
 *
 * RUBRIC COVERAGE:
 * - Unit 4: Multithreading — implements Runnable, runs as a daemon thread.
 * - Unit 4: File I/O — uses BufferedWriter (Character Stream) for logging.
 *
 * VIVA NOTE — DAEMON THREAD:
 * A daemon thread is a "background service" thread. The key property:
 *   The JVM exits when ONLY daemon threads are left running.
 *
 * Normal (non-daemon) threads keep the JVM alive. If our transaction logger
 * was a normal thread, the app would NEVER shut down because the logger is
 * always waiting for new transactions.
 *
 * By setting setDaemon(true), we tell the JVM: "This thread is not important
 * enough to keep the app running. When all user threads finish, just kill this
 * one and exit."
 *
 * This is perfect for background logging — we want it running while the app
 * is alive, but it shouldn't prevent shutdown.
 *
 * HOW IT WORKS:
 * 1. Other threads put Transaction objects into a BlockingQueue.
 * 2. The logger thread takes items from the queue and writes them to a file.
 * 3. BlockingQueue.take() is a BLOCKING call — the thread sleeps (no CPU usage)
 *    until a new item arrives. This is much better than polling (checking repeatedly).
 */
public class TransactionLogger implements Runnable {

    /** Thread-safe queue for incoming transactions — producers put, logger takes */
    private final BlockingQueue<Transaction> logQueue;

    /** Path to the transaction log file */
    private final String logFilePath;

    /** Flag to signal the logger to stop gracefully */
    private volatile boolean running;

    /** The actual Thread object — needed to set it as daemon */
    private Thread loggerThread;

    /** Date format for log timestamps */
    private static final DateTimeFormatter LOG_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Creates a new TransactionLogger.
     *
     * @param logFilePath the path to write transaction logs to
     */
    public TransactionLogger(String logFilePath) {
        this.logQueue = new LinkedBlockingQueue<>();
        this.logFilePath = logFilePath;
        this.running = false;
    }

    /**
     * Starts the logger as a DAEMON THREAD.
     *
     * RUBRIC: Unit 4 — Daemon thread creation and management.
     *
     * VIVA NOTE: The order matters:
     * 1. Create the Thread
     * 2. Call setDaemon(true) BEFORE start()
     * 3. Call start()
     * You CANNOT change a thread to daemon after it has started.
     */
    public void start() {
        if (loggerThread != null && loggerThread.isAlive()) {
            System.out.println("TransactionLogger is already running.");
            return;
        }

        this.running = true;
        this.loggerThread = new Thread(this, "TransactionLogger-Daemon");

        // CRITICAL: Must set daemon BEFORE calling start()
        this.loggerThread.setDaemon(true);

        this.loggerThread.start();
        System.out.println("[TransactionLogger] Daemon thread started. Logging to: " + logFilePath);
    }

    /**
     * Stops the logger gracefully.
     * Sets the running flag to false and interrupts the blocking take() call.
     */
    public void stop() {
        this.running = false;
        if (loggerThread != null) {
            loggerThread.interrupt(); // Wake up the thread if it's blocked on take()
        }
        System.out.println("[TransactionLogger] Daemon thread stopped.");
    }

    /**
     * Enqueues a transaction for logging.
     * This method is called from CLIENT HANDLER threads (multiple producers).
     * BlockingQueue is thread-safe, so no synchronized needed here.
     *
     * @param transaction the transaction to log
     */
    public void log(Transaction transaction) {
        if (transaction != null) {
            logQueue.offer(transaction); // offer() never blocks (unbounded queue)
        }
    }

    /**
     * The main loop of the daemon thread.
     *
     * RUBRIC: Unit 4 — Runnable implementation.
     *
     * This method runs continuously on the daemon thread:
     * 1. Waits for a transaction (blocks on take())
     * 2. Writes it to the log file
     * 3. Repeats until stopped
     */
    @Override
    public void run() {
        // Use try-with-resources for auto-closing, and append mode (true)
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(logFilePath, true))) {

            // Write a header line when the logger starts
            writer.write("=== TransactionLogger started at " +
                    LocalDateTime.now().format(LOG_TIME_FORMAT) + " ===");
            writer.newLine();
            writer.flush();

            // Main loop — runs until stopped
            while (running) {
                try {
                    // BLOCKING CALL: thread sleeps here until a transaction arrives
                    // This is efficient — no CPU usage while waiting
                    Transaction transaction = logQueue.take();

                    // Format the log entry
                    // RUBRIC: Unit 3 — StringBuilder for string construction
                    StringBuilder logEntry = new StringBuilder();
                    logEntry.append("[").append(LocalDateTime.now().format(LOG_TIME_FORMAT)).append("] ");
                    logEntry.append("TXN: ").append(transaction.getTransactionId());
                    logEntry.append(" | Account: ").append(transaction.getAccountNumber());
                    logEntry.append(" | Type: ").append(transaction.getType().getLabel());
                    logEntry.append(" | Amount: ₹").append(String.format("%.2f", transaction.getAmount()));
                    logEntry.append(" | Balance After: ₹").append(String.format("%.2f", transaction.getBalanceAfter()));
                    if (!transaction.getRemarks().isEmpty()) {
                        logEntry.append(" | Remarks: ").append(transaction.getRemarks());
                    }

                    // Write to file
                    writer.write(logEntry.toString());
                    writer.newLine();
                    writer.flush(); // Flush immediately to ensure log is written

                } catch (InterruptedException e) {
                    // Thread was interrupted (probably during shutdown)
                    // Set the interrupt flag back and exit the loop
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Write a footer when shutting down
            writer.write("=== TransactionLogger stopped at " +
                    LocalDateTime.now().format(LOG_TIME_FORMAT) + " ===");
            writer.newLine();
            writer.flush();

        } catch (IOException e) {
            System.err.println("[TransactionLogger] ERROR: Could not write to log file: " + e.getMessage());
        }

        System.out.println("[TransactionLogger] Daemon thread exiting.");
    }

    /**
     * Returns the number of transactions waiting to be logged.
     * Useful for monitoring/debugging.
     */
    public int getPendingCount() {
        return logQueue.size();
    }

    /**
     * Checks if the logger is currently running.
     */
    public boolean isRunning() {
        return running && loggerThread != null && loggerThread.isAlive();
    }
}
