package com.securebank.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTest {

    private Session session;

    @BeforeEach
    void setUp() {
        session = new Session();
    }

    @Test
    @DisplayName("A fresh session is unauthenticated")
    void freshSessionIsUnauthenticated() {
        assertFalse(session.isAuthenticated());
        assertEquals(null, session.getCustomerId());
    }

    @Test
    @DisplayName("authenticate binds the session and clears failed attempts")
    void authenticateBindsSession() {
        for (int i = 0; i < 3; i++) {
            session.recordFailedLogin();
        }
        session.authenticate("CUSTOMER-1", "Divyansh");

        assertTrue(session.isAuthenticated());
        assertEquals("CUSTOMER-1", session.getCustomerId());
        assertEquals("Divyansh", session.getCustomerName());
        // The previously accumulated failures are wiped
        assertFalse(session.isLockedOut());
    }

    @Test
    @DisplayName("Lockout triggers after MAX_FAILED_LOGINS failures")
    void lockoutAfterMaxFailures() {
        assertFalse(session.isLockedOut());
        for (int i = 0; i < Session.MAX_FAILED_LOGINS - 1; i++) {
            assertFalse(session.recordFailedLogin(), "no lockout before the limit");
        }
        assertTrue(session.recordFailedLogin(), "the Nth failure locks the session");
        assertTrue(session.isLockedOut());
        assertTrue(session.getLockoutRemainingSeconds() > 0);
    }

    @Test
    @DisplayName("An unauthenticated session can never be locked into being authenticated")
    void lockoutDoesNotGrantAuth() {
        for (int i = 0; i < Session.MAX_FAILED_LOGINS; i++) {
            session.recordFailedLogin();
        }
        assertTrue(session.isLockedOut());
        assertFalse(session.isAuthenticated(), "lockout must not imply authentication");
    }
}
