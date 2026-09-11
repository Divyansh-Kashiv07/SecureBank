package com.securebank.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * PasswordHasher — salted PBKDF2-HMAC-SHA256 hashing for customer PINs.
 *
 * SECURITY DESIGN:
 * - PINs are NEVER stored in plaintext. Each PIN gets a unique random 16-byte
 *   salt, so identical PINs produce different stored values (defeats rainbow
 *   tables and cross-customer comparison).
 * - PBKDF2 is built into the JDK (no new dependency) and is deliberately CPU-
 *   expensive, so an offline attack on a stolen customers.dat is slow.
 * - Verification uses MessageDigest.isEqual — a constant-time comparison that
 *   does not leak how much of the hash matched (defeats timing attacks).
 * - Stored format: "pbkdf2:iterations:saltB64:hashB64" so the iteration count
 *   can be raised later without breaking old records (legacy plaintext values
 *   are detected by the missing prefix and transparently upgraded on login).
 *
 * PARAMETER CHOICE: 120,000 iterations. OWASP recommends higher for web
 * password storage; this project hashes 4-digit PINs over a socket where login
 * must stay responsive on classroom machines. The cost is still ~3 orders of
 * magnitude above a naive hash. The iteration count is a stored parameter, so
 * it can be increased without data migration.
 */
public final class PasswordHasher {

    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int ITERATIONS = 120_000;
    private static final String PREFIX = "pbkdf2";
    private static final String SEPARATOR = ":";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
        // Utility class — no instances
    }

    /**
     * Hashes a PIN with a fresh random salt.
     *
     * @param pin the plaintext PIN
     * @return the storable string "pbkdf2:iterations:saltB64:hashB64"
     */
    public static String hash(String pin) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = pbkdf2(pin, salt, ITERATIONS);
        return PREFIX + SEPARATOR + ITERATIONS
                + SEPARATOR + Base64.getEncoder().encodeToString(salt)
                + SEPARATOR + Base64.getEncoder().encodeToString(key);
    }

    /**
     * Verifies a PIN against a stored hash (or legacy plaintext value).
     *
     * @param pin    the candidate plaintext PIN
     * @param stored the stored value (hashed or legacy plaintext)
     * @return true if the PIN matches
     */
    public static boolean verify(String pin, String stored) {
        if (pin == null || stored == null) {
            return false;
        }
        if (!isHashed(stored)) {
            // Legacy plaintext record — constant-time compare, then the caller
            // upgrades the record to a salted hash on successful login
            return MessageDigest.isEqual(
                    pin.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        String[] parts = stored.split(SEPARATOR.replace(":", "\\:"));
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedKey = Base64.getDecoder().decode(parts[3]);
            byte[] actualKey = pbkdf2(pin, salt, iterations);
            return MessageDigest.isEqual(expectedKey, actualKey);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks whether a stored value is in the hashed format.
     *
     * @param stored the stored value
     * @return true if it starts with the "pbkdf2:" prefix
     */
    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX + SEPARATOR);
    }

    private static byte[] pbkdf2(String pin, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    pin.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // PBKDF2WithHmacSHA256 is mandatory for every compliant JDK — this
            // cannot happen in practice, but a hashing failure must never
            // silently produce an empty password
            throw new IllegalStateException("PIN hashing unavailable", e);
        }
    }
}
