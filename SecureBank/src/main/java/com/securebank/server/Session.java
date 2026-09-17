package com.securebank.server;

/**
 * Session — per-connection authentication state for one ClientHandler.
 *
 * SECURITY MODEL:
 * Each TCP connection gets exactly one Session. LOGIN authenticates the
 * CONNECTION; every subsequent command on that connection is then authorized
 * against the session's customer. This means:
 * - Unauthenticated connections can only call LOGIN (or QUIT/disconnect).
 * - A connection can never act for two different customers — the session is
 *   bound on first successful LOGIN and cannot be switched.
 * - Repeated login failures lock the session temporarily, slowing online
 *   PIN guessing without any GUI or protocol changes.
 *
 * (A real deployment would add signed session tokens and idle timeouts;
 * see the production-readiness report for the documented next step.)
 */
public class Session {

    /** Failed logins before the session locks */
    public static final int MAX_FAILED_LOGINS = 5;

    /** How long a locked session refuses logins */
    public static final long LOCKOUT_MILLIS = 5 * 60 * 1000L;

    private String customerId;
    private String customerName;
    private int failedLogins;
    private long lockedUntil;

    /**
     * Returns the authenticated customer ID, or null if not authenticated.
     */
    public synchronized String getCustomerId() {
        return customerId;
    }

    /**
     * Returns the authenticated customer's display name, or null.
     */
    public synchronized String getCustomerName() {
        return customerName;
    }

    /**
     * Returns true if this connection has successfully logged in.
     */
    public synchronized boolean isAuthenticated() {
        return customerId != null;
    }

    /**
     * Binds the session to a customer after a successful LOGIN.
     */
    public synchronized void authenticate(String customerId, String customerName) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.failedLogins = 0;
    }

    /**
     * Records a failed login attempt.
     *
     * @return true if this failure triggered a lockout
     */
    public synchronized boolean recordFailedLogin() {
        failedLogins++;
        if (failedLogins >= MAX_FAILED_LOGINS) {
            lockedUntil = System.currentTimeMillis() + LOCKOUT_MILLIS;
            return true;
        }
        return false;
    }

    /**
     * Returns true while the session is locked out from login attempts.
     */
    public synchronized boolean isLockedOut() {
        if (lockedUntil == 0) {
            return false;
        }
        if (System.currentTimeMillis() >= lockedUntil) {
            lockedUntil = 0;      // Lockout expired
            failedLogins = 0;     // Fresh start
            return false;
        }
        return true;
    }

    /**
     * Returns the remaining lockout time in seconds (rounded up).
     */
    public synchronized long getLockoutRemainingSeconds() {
        if (!isLockedOut()) {
            return 0;
        }
        long remaining = lockedUntil - System.currentTimeMillis();
        return (remaining + 999) / 1000;
    }
}
