package com.securebank.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Protocol-level coverage for the saved-payee commands, verified against a real
 * BankServer over a real socket:
 * - the commands are behind the login gate like every other command
 * - the first-run seed data is listed for its owner
 * - add → list → remove round-trips over the wire
 * - payee removal is scoped to the owning customer
 */
class BeneficiaryIntegrationTest extends ServerHarness {

    @Test
    @DisplayName("Payee commands require authentication")
    void requiresLogin() throws Exception {
        ProtocolClient client = newClient();
        assertEquals("ERROR|Authentication required. LOGIN first.",
                client.send("BENEFICIARY_LIST"));
        assertEquals("ERROR|Authentication required. LOGIN first.",
                client.send("BENEFICIARY_ADD|Someone|ACC-009999|HDFC|Nick"));
        client.close();
    }

    @Test
    @DisplayName("Seeded payees are listed for their owner only")
    void seededPayeesAreListed() throws Exception {
        ProtocolClient owner = newClient();
        owner.sendOk("LOGIN|CUSTOMER-1|1234");
        String listed = owner.sendOk("BENEFICIARY_LIST");
        assertTrue(listed.contains("ACC-001003"), "the seeded payee must be listed: " + listed);
        assertTrue(listed.contains("Priya"));
        owner.close();

        // CUSTOMER-2 has no saved payees of their own and must not see CUSTOMER-1's
        ProtocolClient other = newClient();
        other.sendOk("LOGIN|CUSTOMER-2|5678");
        assertEquals("EMPTY", other.sendOk("BENEFICIARY_LIST"));
        other.close();
    }

    @Test
    @DisplayName("A payee added over the wire is listed, then removable")
    void addListRemoveRoundTrip() throws Exception {
        ProtocolClient client = newClient();
        client.sendOk("LOGIN|CUSTOMER-2|5678");

        String added = client.sendOk("BENEFICIARY_ADD|Rahul Verma|ACC-001004|HDFC Bank|Rent");
        assertTrue(added.startsWith("BENE-"), added);

        String listed = client.sendOk("BENEFICIARY_LIST");
        assertTrue(listed.contains(added), "the new payee must be listed");
        assertTrue(listed.contains("Rahul Verma"));
        assertTrue(listed.contains("HDFC Bank"));
        assertTrue(listed.contains("Rent"));

        assertEquals("Beneficiary removed", client.sendOk("BENEFICIARY_REMOVE|" + added));
        assertFalse(client.sendOk("BENEFICIARY_LIST").contains(added),
                "a removed payee must disappear from the list");
        client.close();
    }

    @Test
    @DisplayName("Payee validation and ownership are enforced server-side")
    void validationAndOwnership() throws Exception {
        ProtocolClient client = newClient();
        client.sendOk("LOGIN|CUSTOMER-2|5678");

        // Bad arguments
        assertEquals("ERROR|Usage: BENEFICIARY_ADD|name|accountNumber|bank|nickname",
                client.send("BENEFICIARY_ADD|OnlyName|ACC-001004|HDFC"));
        assertEquals("ERROR|Payee name and account number are required",
                client.send("BENEFICIARY_ADD|  |ACC-001004|HDFC|Nick"));
        assertEquals("ERROR|That account belongs to you — no need to save it as a payee",
                client.send("BENEFICIARY_ADD|Me|ACC-001003|SecureBank|Me"));

        // A payee owned by CUSTOMER-1 cannot be removed by CUSTOMER-2.
        // Use an authenticated CUSTOMER-1 session to add a fresh payee and read its
        // id from the same session, so we never race the seed id sequence.
        ProtocolClient owner = newClient();
        owner.sendOk("LOGIN|CUSTOMER-1|1234");
        String ownedId = owner.sendOk("BENEFICIARY_ADD|Stranger|ACC-001005|NICICI|");
        owner.close();

        assertEquals("ERROR|Not authorized to remove this beneficiary",
                client.send("BENEFICIARY_REMOVE|" + ownedId));
        client.close();

        // An id that has never been registered always fails
        ProtocolClient stranger = newClient();
        stranger.sendOk("LOGIN|CUSTOMER-2|5678");
        assertEquals("ERROR|Beneficiary not found: BENE-999999",
                stranger.send("BENEFICIARY_REMOVE|BENE-999999"));
        stranger.close();
    }
}
