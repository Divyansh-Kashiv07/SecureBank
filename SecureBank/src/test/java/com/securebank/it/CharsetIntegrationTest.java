package com.securebank.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-level charset regression tests.
 *
 * The socket reader/writer on BOTH ends (ClientHandler, BankClient) and the
 * transaction log must be UTF-8: the windows-1252 platform default mangles
 * ₹ (U+20B9) and Devanagari into '?'. These tests prove non-ASCII text
 * round-trips through live server responses and lands intact in the log file.
 */
class CharsetIntegrationTest extends ServerHarness {

    private static final String RUPEE = "₹";
    private static final String DEVANAGARI_REMARK = "₹ भुगतान सफल";

    @Test
    @DisplayName("Server error messages carry ₹ intact over the wire")
    void errorMessagesRoundTripRupee() throws Exception {
        try (ProtocolClient client = newClient()) {
            client.sendOk("LOGIN|CUSTOMER-1|1234");

            // Daily-limit error embeds the formatted limit "₹50000.00"
            String err = client.sendError("WITHDRAW|ACC-001001|999999");
            assertTrue(err.contains(RUPEE + "50000.00"),
                    "₹ was mangled in wire error message: " + err);
        }
    }

    @Test
    @DisplayName("Client-supplied Devanagari remark round-trips through DEPOSIT → HISTORY")
    void devanagariRemarkRoundTripsOverWire() throws Exception {
        try (ProtocolClient client = newClient()) {
            client.sendOk("LOGIN|CUSTOMER-1|1234");

            client.sendOk("DEPOSIT|ACC-001001|101|" + DEVANAGARI_REMARK);

            String history = client.sendOk("HISTORY|ACC-001001");
            assertTrue(history.contains(DEVANAGARI_REMARK),
                    "Devanagari remark did not survive the wire round-trip: " + history);
        }
    }

    @Test
    @DisplayName("Transaction log file persists non-ASCII as valid UTF-8")
    void transactionLogFileIsUtf8() throws Exception {
        String marker = "UTF8-MARKER-" + System.nanoTime() + " " + DEVANAGARI_REMARK;
        try (ProtocolClient client = newClient()) {
            client.sendOk("LOGIN|CUSTOMER-1|1234");
            client.sendOk("DEPOSIT|ACC-001001|102|" + marker);
        }

        // Logger writes asynchronously — poll briefly for the entry to land
        Path log = Paths.get(System.getProperty("securebank.data.dir"), "transaction_log.txt");
        long deadline = System.currentTimeMillis() + 5_000;
        String content = Files.exists(log) ? readUtf8(log) : "";
        while (!content.contains(marker) && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
            content = Files.exists(log) ? readUtf8(log) : "";
        }
        assertTrue(content.contains(marker),
                "Transaction log did not persist the non-ASCII remark as UTF-8");
    }

    private static String readUtf8(Path path) throws Exception {
        // Decoding strict UTF-8 would throw on mojibake '?'; contains() proves real encoding
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
