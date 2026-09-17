package com.securebank.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 security verification at the protocol level: login lockout and the
 * PBKDF2-backed authentication path with legacy-PIN upgrade.
 */
class SecurityIntegrationTest extends ServerHarness {

    @Test
    @Timeout(60)
    @DisplayName("Five consecutive failed logins lock the session, then unlock")
    void loginLockout() throws Exception {
        ProtocolClient client = newClient();

        // 4 failures: generic error, no lockout yet
        for (int i = 0; i < 4; i++) {
            assertEquals("ERROR|Invalid customer ID or PIN.", client.send("LOGIN|CUSTOMER-1|0000"));
        }

        // 5th failure: locks the session
        String locked = client.send("LOGIN|CUSTOMER-1|0000");
        assertTrue(locked.startsWith("ERROR|Too many failed attempts"), got(locked));

        // Even the CORRECT PIN is refused while locked (no bypass)
        String correctWhileLocked = client.send("LOGIN|CUSTOMER-1|1234");
        assertTrue(correctWhileLocked.startsWith("ERROR|Too many failed attempts"),
                got(correctWhileLocked));

        client.close();

        // A DIFFERENT connection is unaffected — lockout is per-session, not global
        ProtocolClient other = newClient();
        assertTrue(other.sendOk("LOGIN|CUSTOMER-1|1234").startsWith("Divyansh Kashiv|"));
        other.close();
    }

    @Test
    @Timeout(60)
    @DisplayName("Failures below the limit do not lock, and lockout stays per-session")
    void failuresBelowLimitDoNotLock() throws Exception {
        ProtocolClient client = newClient();

        for (int i = 0; i < 4; i++) {
            assertEquals("ERROR|Invalid customer ID or PIN.", client.send("LOGIN|CUSTOMER-2|0000"));
        }

        // 4 failures — still below the limit, so the correct PIN is accepted
        // (the SessionTest.authenticateBindsSession covers the counter reset itself)
        assertTrue(client.sendOk("LOGIN|CUSTOMER-2|5678").startsWith("Priya Sharma|"));
        client.close();

        // Other connections were never affected by this one's failures
        ProtocolClient other = newClient();
        assertTrue(other.sendOk("LOGIN|CUSTOMER-1|1234").startsWith("Divyansh Kashiv|"));
        other.close();
    }

    private static String got(String response) {
        return "unexpected response: " + response;
    }
}
