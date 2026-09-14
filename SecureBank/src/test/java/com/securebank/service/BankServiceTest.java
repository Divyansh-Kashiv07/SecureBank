package com.securebank.service;

import com.securebank.core.Beneficiary;
import com.securebank.core.Customer;
import com.securebank.core.SavingsAccount;
import com.securebank.loans.LoanProcessor;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.BeneficiaryRepository;
import com.securebank.repository.CustomerRepository;
import com.securebank.transactions.TransactionLogger;
import com.securebank.utils.PasswordHasher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the application service layer — no sockets, no server.
 *
 * These exercise the rules the protocol tests cannot reach cheaply: the exact
 * validation messages, the ownership boundary on payee removal, and the
 * guarantee that user input can never corrupt the record encoding.
 */
class BankServiceTest {

    private static String dataDir;
    private BankService service;

    @BeforeAll
    static void isolateDataDirectory() throws Exception {
        dataDir = Files.createTempDirectory("securebank-service").toString();
        System.setProperty("securebank.data.dir", dataDir);
    }

    @BeforeEach
    void setUp() {
        AccountRepository accounts = new AccountRepository();
        CustomerRepository customers = new CustomerRepository();
        BeneficiaryRepository beneficiaries = new BeneficiaryRepository();

        Customer alice = new Customer("CUSTOMER-1", "Divyansh Kashiv", "a@securebank.com",
                "9876543210", "Greater Noida", PasswordHasher.hash("1234"));
        alice.addAccount("ACC-001001");
        customers.addCustomer(alice);

        Customer bob = new Customer("CUSTOMER-2", "Priya Sharma", "b@securebank.com",
                "9876543211", "Noida", PasswordHasher.hash("5678"));
        bob.addAccount("ACC-001003");
        customers.addCustomer(bob);

        accounts.addAccount(new SavingsAccount("ACC-001001", "Divyansh Kashiv", "CUSTOMER-1", 25000.0));
        accounts.addAccount(new SavingsAccount("ACC-001003", "Priya Sharma", "CUSTOMER-2", 15000.0));

        service = new BankService(accounts, customers, new LoanProcessor(), beneficiaries,
                new TransactionLogger(dataDir + "/service_txn_log.txt"));
    }

    @Test
    @DisplayName("A saved payee can be listed and removed")
    void addListRemove() {
        String added = service.addBeneficiary("CUSTOMER-1", "Rahul Verma", "ACC-001004", "HDFC", "Rent");
        assertTrue(added.startsWith("OK|BENE-"), added);
        String id = added.substring(3);

        String listed = service.getBeneficiaries("CUSTOMER-1");
        assertTrue(listed.startsWith("OK|"));
        assertTrue(listed.contains("Rahul Verma"));
        assertTrue(listed.contains("ACC-001004"));

        assertEquals("OK|Beneficiary removed", service.removeBeneficiary("CUSTOMER-1", id));
        assertEquals("OK|EMPTY", service.getBeneficiaries("CUSTOMER-1"));
    }

    @Test
    @DisplayName("Duplicate payees and the customer's own account are rejected")
    void rejectsDuplicatesAndOwnAccounts() {
        service.addBeneficiary("CUSTOMER-1", "Rahul Verma", "ACC-001004", "HDFC", "");

        assertEquals("ERROR|That account is already saved as a payee",
                service.addBeneficiary("CUSTOMER-1", "Someone Else", "ACC-001004", "ICICI", ""));
        assertEquals("ERROR|That account belongs to you — no need to save it as a payee",
                service.addBeneficiary("CUSTOMER-1", "Me", "ACC-001001", "SecureBank", ""));
    }

    @Test
    @DisplayName("A payee needs both a name and an account number")
    void requiresNameAndAccount() {
        assertEquals("ERROR|Payee name and account number are required",
                service.addBeneficiary("CUSTOMER-1", "   ", "ACC-001004", "HDFC", ""));
        assertEquals("ERROR|Payee name and account number are required",
                service.addBeneficiary("CUSTOMER-1", "Rahul", "", "HDFC", ""));
    }

    @Test
    @DisplayName("One customer can never remove another customer's payee")
    void removalIsAuthorized() {
        String id = service.addBeneficiary("CUSTOMER-1", "Rahul", "ACC-001004", "HDFC", "").substring(3);

        assertEquals("ERROR|Not authorized to remove this beneficiary",
                service.removeBeneficiary("CUSTOMER-2", id));
        assertTrue(service.getBeneficiaries("CUSTOMER-1").contains(id),
                "a refused removal must leave the payee in place");

        assertEquals("ERROR|Beneficiary not found: BENE-999999",
                service.removeBeneficiary("CUSTOMER-1", "BENE-999999"));
    }

    @Test
    @DisplayName("Delimiters in user input cannot corrupt the record encoding")
    void inputDelimitersAreSanitized() {
        String id = service.addBeneficiary("CUSTOMER-1",
                "Bad|Name;Injected", "ACC-001004", "HDFC", "nick;name").substring(3);

        assertEquals(1, service.getBeneficiaries("CUSTOMER-1").substring(3).split(";").length,
                "a name containing the record separator must not create a second record");

        // The sanitized values survive a real disk round trip
        BeneficiaryRepository reloaded = new BeneficiaryRepository();
        reloaded.loadFromFile();
        Beneficiary stored = reloaded.getBeneficiary(id);
        assertNotNull(stored);
        assertEquals("Bad Name Injected", stored.getName());
        assertEquals("nick name", stored.getNickname());
    }

    @Test
    @DisplayName("Authentication answers the same way for unknown users and wrong PINs")
    void authenticationNeverRevealsWhichPartWasWrong() {
        assertNull(service.authenticate("CUSTOMER-1", "0000"), "wrong PIN must fail");
        assertNull(service.authenticate("CUSTOMER-999", "1234"), "unknown user must fail");
        assertNull(service.authenticate("CUSTOMER-1", ""), "empty PIN must fail");
        assertNotNull(service.authenticate("CUSTOMER-1", "1234"), "correct PIN must succeed");
    }
}
