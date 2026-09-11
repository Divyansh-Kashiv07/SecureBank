package com.securebank.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end protocol tests against a REAL BankServer running the PHASE 3
 * security model:
 * - LOGIN is mandatory before any other command (per-connection session)
 * - Every command is authorized against the session customer (IDOR protection)
 * - Auth failures return generic messages (no user enumeration)
 * - SAVE is rejected for clients; QUIT ends the connection cleanly
 */
class ProtocolIntegrationTest extends ServerHarness {

    @Test
    @DisplayName("Commands before LOGIN are refused")
    void requiresLogin() throws Exception {
        ProtocolClient client = newClient();

        String resp = client.send("BALANCE|ACC-001001");
        assertTrue(resp.startsWith("ERROR|"), "unauthenticated commands must be refused");
        assertTrue(resp.toLowerCase().contains("auth"));

        client.close();
    }

    @Test
    @DisplayName("LOGIN returns customer name and account list; failures are generic")
    void login() throws Exception {
        // Successful login
        ProtocolClient client = newClient();
        String ok = client.sendOk("LOGIN|CUSTOMER-1|1234");
        assertTrue(ok.startsWith("Divyansh Kashiv|"));
        client.close();

        // Failed logins happen on their own (unauthenticated) connections —
        // an authenticated session cannot retry LOGIN
        ProtocolClient badPinClient = newClient();
        String badPin = badPinClient.send("LOGIN|CUSTOMER-1|0000");
        assertTrue(badPin.startsWith("ERROR|"));
        assertFalse(badPin.toLowerCase().contains("1234"));
        assertEquals("ERROR|Invalid customer ID or PIN.",
                badPin, "wrong PIN and unknown user must be indistinguishable");
        badPinClient.close();

        ProtocolClient noUserClient = newClient();
        String noUser = noUserClient.send("LOGIN|CUSTOMER-999|1234");
        assertEquals("ERROR|Invalid customer ID or PIN.", noUser);
        noUserClient.close();
    }

    @Test
    @DisplayName("A session cannot act on another customer's accounts (IDOR fixed)")
    void idorProtection() throws Exception {
        ProtocolClient attacker = newClient();
        attacker.sendOk("LOGIN|CUSTOMER-2|5678");

        // Priya (CUSTOMER-2) must NOT be able to touch Divyansh's accounts
        assertTrue(attacker.send("BALANCE|ACC-001001").startsWith("ERROR|"));
        assertTrue(attacker.send("DEPOSIT|ACC-001001|1000").startsWith("ERROR|"));
        assertTrue(attacker.send("WITHDRAW|ACC-001001|10").startsWith("ERROR|"));
        assertTrue(attacker.send("TRANSFER|ACC-001001|ACC-001003|10").startsWith("ERROR|"),
                "transfers FROM another customer's account must be refused");
        assertTrue(attacker.send("HISTORY|ACC-001001").startsWith("ERROR|"));
        assertTrue(attacker.send("ACCOUNT_INFO|ACC-001001").startsWith("ERROR|"));
        assertTrue(attacker.send("INTEREST|ACC-001001").startsWith("ERROR|"));
        assertTrue(attacker.send("LOAN_APPLY|CUSTOMER-1|ACC-001001|50000|12|fraud")
                .startsWith("ERROR|"), "applying for loans as another customer must fail");
        assertTrue(attacker.send("LOAN_STATUS|CUSTOMER-1").startsWith("ERROR|"));
        assertTrue(attacker.send("ACCOUNTS|CUSTOMER-1").startsWith("ERROR|"));

        // CREATE_ACCOUNT ignores the customerId parameter — the new account belongs
        // to the SESSION customer (CUSTOMER-2), never to the requested CUSTOMER-1
        String newAcc = attacker.sendOk("CREATE_ACCOUNT|CUSTOMER-1|Savings|1000");
        ProtocolClient victim2 = newClient();
        victim2.sendOk("LOGIN|CUSTOMER-1|1234");
        assertTrue(victim2.send("BALANCE|" + newAcc).startsWith("ERROR|"),
                "CREATE_ACCOUNT must never attach an account to another customer");
        victim2.close();

        // Own accounts still work fine
        assertTrue(attacker.send("BALANCE|ACC-001003").startsWith("OK|"));

        // The attack must have moved no money
        ProtocolClient victim = newClient();
        victim.sendOk("LOGIN|CUSTOMER-1|1234");
        double victimBalance = Double.parseDouble(victim.sendOk("BALANCE|ACC-001001"));
        assertEquals(25000.0, victimBalance, 1e-9,
                "attacker's commands must not have changed the victim's balance");

        attacker.close();
        victim.close();
    }

    @Test
    @DisplayName("DEPOSIT credits the account and returns the new balance + transaction ID")
    void deposit() throws Exception {
        ProtocolClient client = newClient();
        client.sendOk("LOGIN|CUSTOMER-2|5678");

        double before = Double.parseDouble(client.sendOk("BALANCE|ACC-001003"));

        String resp = client.sendOk("DEPOSIT|ACC-001003|1000.50|integration test");
        String[] parts = resp.split("\\|");
        assertEquals(2, parts.length);
        assertEquals(before + 1000.50, Double.parseDouble(parts[0]), 1e-9);
        assertTrue(parts[1].startsWith("TXN-"));

        assertEquals(String.format("%.2f", before + 1000.50),
                client.sendOk("BALANCE|ACC-001003"));

        client.close();
    }

    @Test
    @DisplayName("WITHDRAW applies the withdrawal; exceeding available funds is rejected")
    void withdraw() throws Exception {
        ProtocolClient client = newClient();
        client.sendOk("LOGIN|CUSTOMER-2|5678");

        String resp = client.sendOk("WITHDRAW|ACC-001003|500");
        String[] parts = resp.split("\\|");
        assertEquals(2, parts.length);
        assertTrue(parts[1].startsWith("TXN-"));

        String overdraw = client.send("WITHDRAW|ACC-001003|999999999");
        assertTrue(overdraw.startsWith("ERROR|"));

        client.close();
    }

    @Test
    @DisplayName("TRANSFER moves funds between the session customer's accounts")
    void transfer() throws Exception {
        ProtocolClient client = newClient();
        client.sendOk("LOGIN|CUSTOMER-1|1234");

        double fromBefore = Double.parseDouble(client.sendOk("BALANCE|ACC-001001"));
        double toBefore = Double.parseDouble(client.sendOk("BALANCE|ACC-001002"));

        client.sendOk("TRANSFER|ACC-001001|ACC-001002|250");

        assertEquals(fromBefore - 250,
                Double.parseDouble(client.sendOk("BALANCE|ACC-001001")), 1e-9);
        assertEquals(toBefore + 250,
                Double.parseDouble(client.sendOk("BALANCE|ACC-001002")), 1e-9);

        client.close();
    }

    @Test
    @DisplayName("HISTORY returns semicolon-separated entries; ACCOUNT_INFO returns holder data")
    void historyAndInfo() throws Exception {
        ProtocolClient client = newClient();
        client.sendOk("LOGIN|CUSTOMER-3|9012");

        client.sendOk("DEPOSIT|ACC-001004|100|history seed");

        String history = client.sendOk("HISTORY|ACC-001004");
        assertFalse(history.equals("EMPTY"));
        assertTrue(history.contains("DEPOSIT"));

        String info = client.sendOk("ACCOUNT_INFO|ACC-001004");
        String[] infoParts = info.split("\\|");
        assertEquals(4, infoParts.length);
        assertEquals("Rahul Verma", infoParts[0]);
        assertEquals("Savings", infoParts[1]);

        client.close();
    }

    @Test
    @DisplayName("Malformed requests and unknown commands produce ERROR, never a crash")
    void malformedRequests() throws Exception {
        ProtocolClient client = newClient();
        client.sendOk("LOGIN|CUSTOMER-1|1234");

        assertTrue(client.send("BOGUS_COMMAND|x").startsWith("ERROR|"));
        assertTrue(client.send("DEPOSIT|ACC-001001|not-a-number").startsWith("ERROR|"));
        assertTrue(client.send("BALANCE|ACC-DOES-NOT-EXIST").startsWith("ERROR|"));
        assertTrue(client.send("WITHDRAW|ACC-001001|-5").startsWith("ERROR|"));
        assertTrue(client.send("LOGIN|CUSTOMER-2|5678").startsWith("ERROR|"),
                "second LOGIN on an authenticated connection must be refused");

        // Server must still be alive and serving after all that abuse
        assertTrue(client.send("BALANCE|ACC-001001").startsWith("OK|"));

        client.close();
    }

    @Test
    @DisplayName("SAVE is rejected for client connections")
    void saveRejected() throws Exception {
        ProtocolClient client = newClient();
        client.sendOk("LOGIN|CUSTOMER-1|1234");

        assertTrue(client.send("SAVE").startsWith("ERROR|"));

        client.close();
    }

    @Test
    @DisplayName("QUIT ends the connection cleanly")
    void quitEndsConnection() throws Exception {
        ProtocolClient client = newClient();
        client.sendOk("LOGIN|CUSTOMER-1|1234");

        assertEquals("BYE", client.send("QUIT"));

        // Any further request gets no response — the server closed the connection.
        // Expect either an IOException from the closed socket or a null/empty read.
        try {
            String afterQuit = client.send("BALANCE|ACC-001001");
            assertTrue(afterQuit == null || afterQuit.isEmpty(),
                    "connection should be closed after QUIT");
        } catch (java.io.IOException expected) {
            // The server closing the socket is the expected outcome
        }

        client.close();
    }

    @Test
    @DisplayName("Full customer journey: login → deposit → withdraw → transfer → history")
    void fullJourney() throws Exception {
        ProtocolClient client = newClient();
        client.sendOk("LOGIN|CUSTOMER-1|1234");

        client.sendOk("DEPOSIT|ACC-001002|2000|journey deposit");
        client.sendOk("WITHDRAW|ACC-001002|500");
        client.sendOk("TRANSFER|ACC-001002|ACC-001001|300");

        String accounts = client.sendOk("ACCOUNTS|CUSTOMER-1");
        assertTrue(accounts.contains("ACC-001001"));
        assertTrue(accounts.contains("ACC-001002"));

        String history = client.sendOk("HISTORY|ACC-001002");
        assertTrue(history.contains("WITHDRAWAL"));
        assertTrue(history.contains("TRANSFER_OUT"));

        client.close();
    }

    @Test
    @DisplayName("Multiple authenticated clients can operate concurrently")
    void multipleClients() throws Exception {
        ProtocolClient c1 = newClient();
        ProtocolClient c2 = newClient();

        c1.sendOk("LOGIN|CUSTOMER-1|1234");
        c2.sendOk("LOGIN|CUSTOMER-2|5678");

        String r1 = c1.send("BALANCE|ACC-001001");
        String r2 = c2.send("BALANCE|ACC-001003");
        assertTrue(r1.startsWith("OK|"));
        assertTrue(r2.startsWith("OK|"));
        assertTrue(c1.send("DEPOSIT|ACC-001001|10").startsWith("OK|"));
        assertTrue(c2.send("DEPOSIT|ACC-001003|10").startsWith("OK|"));

        c1.close();
        c2.close();
    }
}
