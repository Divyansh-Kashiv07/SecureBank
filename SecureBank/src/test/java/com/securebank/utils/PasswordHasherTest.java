package com.securebank.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    @DisplayName("Hashed PINs verify successfully and reject wrong PINs")
    void hashAndVerify() {
        String stored = PasswordHasher.hash("1234");

        assertTrue(PasswordHasher.verify("1234", stored));
        assertFalse(PasswordHasher.verify("0000", stored));
        assertFalse(PasswordHasher.verify("", stored));
        assertFalse(PasswordHasher.verify(null, stored));
        assertFalse(PasswordHasher.verify("1234", null));
    }

    @Test
    @DisplayName("Salting: identical PINs produce different stored hashes")
    void saltingMakesHashesUnique() {
        String h1 = PasswordHasher.hash("1234");
        String h2 = PasswordHasher.hash("1234");

        assertNotEquals(h1, h2, "identical PINs must not share a stored value");
        assertTrue(PasswordHasher.verify("1234", h1));
        assertTrue(PasswordHasher.verify("1234", h2));
    }

    @Test
    @DisplayName("Stored format is self-describing and contains no plaintext")
    void storedFormat() {
        String stored = PasswordHasher.hash("1234");
        String[] parts = stored.split(":");

        assertEquals(4, parts.length);
        assertEquals("pbkdf2", parts[0]);
        assertEquals("120000", parts[1]);
        assertFalse(stored.contains("1234"), "plaintext must never appear in the stored value");
        assertTrue(PasswordHasher.isHashed(stored));
    }

    @Test
    @DisplayName("Legacy plaintext values verify and are detected for upgrade")
    void legacyPlaintextSupport() {
        assertTrue(PasswordHasher.verify("1234", "1234"), "legacy plaintext must still verify");
        assertFalse(PasswordHasher.verify("0000", "1234"));
        assertFalse(PasswordHasher.isHashed("1234"), "legacy values are detected as not hashed");
    }

    @Test
    @DisplayName("Corrupted hash strings fail verification safely")
    void corruptedHashFailsSafely() {
        assertFalse(PasswordHasher.verify("1234", "pbkdf2:120000:!!!notbase64!!!:alsobad"));
        assertFalse(PasswordHasher.verify("1234", "pbkdf2:garbage:x:y"));
        assertFalse(PasswordHasher.verify("1234", "pbkdf2:120000:only:salt:hash:extra"));
    }
}
