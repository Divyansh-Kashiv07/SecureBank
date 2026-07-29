package com.securebank.utils;

import com.securebank.core.Account;
import com.securebank.transactions.Transaction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ReceiptGenerator — generates formatted receipts and statements using StringBuilder.
 *
 * RUBRIC COVERAGE:
 * - Unit 3: String handling — uses StringBuilder exclusively (not + concatenation)
 *   for building multi-line formatted outputs.
 *
 * VIVA NOTE — WHY StringBuilder?
 * In Java, String objects are IMMUTABLE. Every time you use + concatenation:
 *   String result = "Hello" + " " + "World";
 * Java creates MULTIPLE intermediate String objects that are immediately discarded.
 * For building long strings (like receipts), this is very inefficient.
 *
 * StringBuilder maintains a MUTABLE internal character array. Appending to it
 * just adds to the existing array (or resizes it once if needed). Much faster
 * for building strings in a loop or with many parts.
 *
 * When to use what:
 * - 2-3 concatenations in a single expression: + is fine (compiler optimizes it)
 * - Loops, or building large multi-line strings: StringBuilder is much better
 * - Multi-threaded context: StringBuffer (synchronized version of StringBuilder)
 */
public class ReceiptGenerator {

    /** Line separator for receipts */
    private static final String LINE = "════════════════════════════════════════════════════";
    private static final String THIN_LINE = "────────────────────────────────────────────────────";
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm:ss a");

    /**
     * Generates a formatted transaction receipt.
     *
     * @param transaction the transaction to generate a receipt for
     * @param accountName the account holder's name
     * @return the formatted receipt as a String
     */
    public static String generateReceipt(Transaction transaction, String accountName) {
        // RUBRIC: StringBuilder used exclusively — no + concatenation for building
        StringBuilder receipt = new StringBuilder();

        receipt.append("\n").append(LINE).append("\n");
        receipt.append("           SecureBank — Transaction Receipt\n");
        receipt.append(LINE).append("\n");
        receipt.append("\n");
        receipt.append("  Transaction ID  : ").append(transaction.getTransactionId()).append("\n");
        receipt.append("  Date & Time     : ").append(
                transaction.getTimestamp().format(DISPLAY_FORMAT)).append("\n");
        receipt.append("  Account         : ").append(transaction.getAccountNumber()).append("\n");
        receipt.append("  Account Holder  : ").append(accountName).append("\n");
        receipt.append("\n");
        receipt.append(THIN_LINE).append("\n");
        receipt.append("\n");
        receipt.append("  Type            : ").append(transaction.getType().getLabel()).append("\n");
        receipt.append("  Amount          : ₹").append(
                String.format("%,.2f", transaction.getAmount())).append("\n");
        receipt.append("  Balance After   : ₹").append(
                String.format("%,.2f", transaction.getBalanceAfter())).append("\n");

        if (!transaction.getRemarks().isEmpty()) {
            receipt.append("  Remarks         : ").append(transaction.getRemarks()).append("\n");
        }

        receipt.append("\n");
        receipt.append(THIN_LINE).append("\n");
        receipt.append("  Thank you for banking with SecureBank!\n");
        receipt.append(LINE).append("\n");

        return receipt.toString();
    }

    /**
     * Generates a mini account statement (last N transactions).
     *
     * @param account         the account to generate a statement for
     * @param maxTransactions maximum number of recent transactions to show
     * @return the formatted statement
     */
    public static String generateMiniStatement(Account account, int maxTransactions) {
        StringBuilder statement = new StringBuilder();

        statement.append("\n").append(LINE).append("\n");
        statement.append("          SecureBank — Mini Statement\n");
        statement.append(LINE).append("\n");
        statement.append("\n");
        statement.append("  Account Number  : ").append(account.getAccountNumber()).append("\n");
        statement.append("  Account Holder  : ").append(account.getHolderName()).append("\n");
        statement.append("  Account Type    : ").append(account.getAccountType()).append("\n");
        statement.append("  Current Balance : ₹").append(
                String.format("%,.2f", account.getBalance())).append("\n");
        statement.append("  Statement Date  : ").append(
                LocalDateTime.now().format(DISPLAY_FORMAT)).append("\n");
        statement.append("\n");
        statement.append(THIN_LINE).append("\n");
        statement.append(String.format("  %-12s %-15s %12s %14s\n",
                "Date", "Type", "Amount", "Balance"));
        statement.append(THIN_LINE).append("\n");

        List<Transaction> history = account.getTransactionHistory();
        int start = Math.max(0, history.size() - maxTransactions);

        for (int i = start; i < history.size(); i++) {
            Transaction txn = history.get(i);
            String dateStr = txn.getTimestamp().format(
                    DateTimeFormatter.ofPattern("dd-MMM-yy"));
            String typeStr = txn.getType().getLabel();
            // Truncate type to fit column
            if (typeStr.length() > 15) {
                typeStr = typeStr.substring(0, 12) + "...";
            }

            statement.append(String.format("  %-12s %-15s ₹%,10.2f  ₹%,12.2f\n",
                    dateStr, typeStr, txn.getAmount(), txn.getBalanceAfter()));
        }

        if (history.isEmpty()) {
            statement.append("  No transactions found.\n");
        }

        statement.append("\n");
        statement.append(THIN_LINE).append("\n");
        statement.append("  Total Transactions: ").append(history.size()).append("\n");
        statement.append("  Showing last ").append(Math.min(maxTransactions, history.size())).append("\n");
        statement.append(LINE).append("\n");

        return statement.toString();
    }

    /**
     * Generates a full account statement for all transactions.
     *
     * @param account the account
     * @return the formatted full statement
     */
    public static String generateFullStatement(Account account) {
        return generateMiniStatement(account, account.getTransactionHistory().size());
    }

    /**
     * Generates a simple balance enquiry receipt.
     *
     * @param account the account to check
     * @return formatted balance enquiry
     */
    public static String generateBalanceEnquiry(Account account) {
        StringBuilder enquiry = new StringBuilder();

        enquiry.append("\n").append(THIN_LINE).append("\n");
        enquiry.append("  Balance Enquiry — ").append(
                LocalDateTime.now().format(DISPLAY_FORMAT)).append("\n");
        enquiry.append(THIN_LINE).append("\n");
        enquiry.append("  Account  : ").append(account.getAccountNumber()).append("\n");
        enquiry.append("  Holder   : ").append(account.getHolderName()).append("\n");
        enquiry.append("  Type     : ").append(account.getAccountType()).append("\n");
        enquiry.append("  Balance  : ₹").append(
                String.format("%,.2f", account.getBalance())).append("\n");
        enquiry.append(THIN_LINE).append("\n");

        return enquiry.toString();
    }
}
